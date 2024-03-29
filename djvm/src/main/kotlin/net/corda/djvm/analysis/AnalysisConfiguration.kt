package net.corda.djvm.analysis

import net.corda.djvm.analysis.impl.X500_NAME
import net.corda.djvm.analysis.impl.generateInterfaceBridgeMethods
import net.corda.djvm.analysis.impl.generateJavaAnnotationMethods
import net.corda.djvm.analysis.impl.generateJavaBaseMethods
import net.corda.djvm.analysis.impl.generateJavaBitsMethods
import net.corda.djvm.analysis.impl.generateJavaCalendarMethods
import net.corda.djvm.analysis.impl.generateJavaEnumMethods
import net.corda.djvm.analysis.impl.generateJavaMathMethods
import net.corda.djvm.analysis.impl.generateJavaPackageMethods
import net.corda.djvm.analysis.impl.generateJavaResourceBundleMethods
import net.corda.djvm.analysis.impl.generateJavaTimeMethods
import net.corda.djvm.analysis.impl.generateJavaUuidMethods
import net.corda.djvm.code.impl.CLASS_CONSTRUCTOR_NAME
import net.corda.djvm.code.impl.DJVM_EXCEPTION_NAME
import net.corda.djvm.code.impl.DJVM_NAME
import net.corda.djvm.code.impl.RUNTIME_ACCOUNTER_NAME
import net.corda.djvm.code.impl.SANDBOX_CLASSLOADER_NAME
import net.corda.djvm.code.impl.SANDBOX_CLASS_NAME
import net.corda.djvm.code.impl.SANDBOX_OBJECT_NAME
import net.corda.djvm.code.impl.asPackagePath
import net.corda.djvm.code.impl.asResourcePath
import net.corda.djvm.formatting.MemberFormatter
import net.corda.djvm.messages.Severity
import net.corda.djvm.references.ClassModule
import net.corda.djvm.references.Member
import net.corda.djvm.references.MemberInformation
import net.corda.djvm.references.MemberModule
import net.corda.djvm.source.ApiSource
import net.corda.djvm.source.ClassHeader
import net.corda.djvm.source.EmptyApi
import net.corda.djvm.source.SourceClassLoader
import net.corda.djvm.source.UserSource
import net.corda.djvm.source.impl.SourceClassLoaderImpl
import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.Type
import java.lang.reflect.Modifier
import java.nio.ByteOrder
import java.util.Collections.unmodifiableList
import java.util.Collections.unmodifiableMap
import java.util.Collections.unmodifiableSet
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.Comparator
import kotlin.collections.LinkedHashSet

/**
 * The configuration to use for an analysis.
 *
 * @property parent This configuration's parent [AnalysisConfiguration].
 * @property whitelist The whitelist of class names.
 * @property stitchedAnnotations Descriptors for annotation classes whose unsandboxed values must also be preserved.
 * @property minimumSeverityLevel The minimum severity level to log and report.
 * @property supportingClassLoader ClassLoader providing the classes to run inside the sandbox.
 * @property classResolver Functionality used to resolve the qualified name and relevant information about a class.
 * @property syntheticResolver Resolves the internal names of synthetic "friend" classes.
 * @property analyzeAnnotations Analyze annotations despite not being explicitly referenced.
 * @property prefixFilters Only record messages where the originating class name matches one of the provided prefixes.
 * If none are provided, all messages will be reported.
 * @property classModule Module for handling evolution of a class hierarchy during analysis.
 * @property memberModule Module for handling the specification and inspection of class members.
 */
class AnalysisConfiguration private constructor(
    val parent: AnalysisConfiguration?,
    val whitelist: Whitelist,
    val stitchedAnnotations: Set<String>,
    val minimumSeverityLevel: Severity,
    val supportingClassLoader: SourceClassLoader,
    val classResolver: ClassResolver,
    val syntheticResolver: SyntheticResolver,
    val analyzeAnnotations: Boolean,
    val prefixFilters: List<String>,
    val classModule: ClassModule,
    val memberModule: MemberModule
) {

    fun formatFor(member: MemberInformation): String = MemberFormatter(classModule, memberModule).format(member)

    /**
     * These interfaces are modified as they are mapped into the sandbox by
     * having their unsandboxed version "stitched in" as a super-interface.
     * And in some cases, we need to add some synthetic bridge methods as well.
     */
    val stitchedInterfaces: Map<String, List<Member>> get() = STITCHED_INTERFACES

    /**
     * These classes have extra methods added as they are mapped into the sandbox.
     */
    val stitchedClasses: Map<String, List<Member>> get() = STITCHED_CLASSES

    fun isImmutable(className: String): Boolean = className in IMMUTABLE_CLASSES
    fun isJvmException(className: String): Boolean = className in JVM_EXCEPTIONS
    fun isJvmAnnotation(className: String): Boolean = className in JVM_ANNOTATIONS
    fun hasDJVMSynthetic(className: String): Boolean = !isJvmException(className) && !isJvmAnnotation(className)

    fun isJvmAnnotationDesc(descriptor: String): Boolean = descriptor in JVM_ANNOTATION_DESC

    fun toSandboxClassName(header: ClassHeader): String {
        val sandboxName = classResolver.resolve(header.internalName)
        return when {
            header.isThrowable -> syntheticResolver.getRealThrowableName(sandboxName)
            else -> sandboxName
        }
    }

    @Throws(ClassNotFoundException::class)
    fun getSourceHeader(internalClassName: String): ClassHeader {
        return supportingClassLoader.loadClassHeader(internalClassName.asPackagePath)
    }

    interface Builder : AnalysisOptions {
        fun build(): AnalysisConfiguration
    }

    /**
     * Creates a [Builder], which will build a child [AnalysisConfiguration]
     * with this instance as its parent. The child inherits the same [whitelist].
     */
    fun createChild(userSource: UserSource): Builder = AnalysisChildBuilder(userSource)

    private inner class AnalysisChildBuilder(private val userSource: UserSource): Builder {
        private val visibleAnnotations = linkedSetOf<Class<out Annotation>>()
        private var newMinimumSeverityLevel = minimumSeverityLevel

        override fun setMinimumSeverityLevel(level: Severity) {
            newMinimumSeverityLevel = level
        }

        override fun setVisibleAnnotations(annotations: Iterable<Class<out Annotation>>) {
            visibleAnnotations.addAll(annotations)
        }

        override fun build(): AnalysisConfiguration {
            return AnalysisConfiguration(
                parent = this@AnalysisConfiguration,
                whitelist = whitelist,
                stitchedAnnotations = stitchedAnnotations.merge(visibleAnnotations),
                classResolver = classResolver,
                syntheticResolver = syntheticResolver,
                minimumSeverityLevel = minimumSeverityLevel,
                analyzeAnnotations = analyzeAnnotations,
                prefixFilters = prefixFilters,
                classModule = classModule,
                memberModule = memberModule,
                supportingClassLoader = SourceClassLoaderImpl(classResolver, userSource, EmptyApi, supportingClassLoader)
            )
        }
    }

    companion object {
        /**
         * The package name prefix to use for classes loaded into a sandbox.
         */
        const val SANDBOX_PREFIX: String = "sandbox/"

        /**
         * The unsandboxed descriptor for Kotlin's [Metadata] annotation.
         */
        const val KOTLIN_METADATA = "Lkotlin/Metadata;"

        private const val SHARED_SECRETS = "sandbox/sun/misc/SharedSecrets"

        /**
         * These annotations are duplicated into the sandbox, such
         * that the sandboxed class is annotated with both the original
         * annotation and the transformed one.
         */
        private val STITCHED_ANNOTATIONS: Set<String> = unmodifiable(setOf(
            KOTLIN_METADATA
        ))

        /**
         * These classes will be duplicated into every sandbox's
         * parent classloader.
         */
        private val TEMPLATE_CLASSES: Set<String> = unmodifiable(setOf(
            java.lang.Boolean::class.java,
            java.lang.Byte::class.java,
            java.lang.Character::class.java,
            java.lang.Double::class.java,
            java.lang.Float::class.java,
            java.lang.Integer::class.java,
            java.lang.Long::class.java,
            java.lang.Runtime::class.java,
            java.lang.Short::class.java,
            java.lang.StrictMath::class.java,
            java.lang.String::class.java,
            java.lang.String.CASE_INSENSITIVE_ORDER::class.java,
            java.lang.System::class.java,
            java.lang.ThreadLocal::class.java,
            java.lang.Throwable::class.java,
            java.lang.ref.Reference::class.java,
            java.security.AccessController::class.java,
            java.util.ServiceLoader::class.java,
            java.util.concurrent.ConcurrentHashMap::class.java,
            java.util.concurrent.ConcurrentHashMap.KeySetView::class.java,
            java.util.concurrent.atomic.AtomicInteger::class.java,
            java.util.concurrent.atomic.AtomicLong::class.java,
            java.util.concurrent.atomic.AtomicReference::class.java,
            java.util.concurrent.locks.ReentrantLock::class.java,
            java.util.zip.CRC32::class.java,
            java.util.zip.Inflater::class.java
        ).sandboxed() + setOf(
            "sandbox/BasicInput",
            "sandbox/BasicOutput",
            "sandbox/ImportSupplierTask",
            "sandbox/ImportTask",
            "sandbox/PredicateTask",
            "sandbox/RawTask",
            "sandbox/Task",
            "sandbox/TaskTypes",
            RUNTIME_ACCOUNTER_NAME,
            "sandbox/java/io/DJVMInputStream",
            "sandbox/java/lang/Character\$Cache",
            DJVM_NAME,
            "sandbox/java/lang/DJVMAnnotationAction",
            "sandbox/java/lang/DJVMAnnotationsByTypeAction",
            "sandbox/java/lang/DJVMAnnotationHandler",
            "sandbox/java/lang/DJVMAnnotationHandler\$MethodValue",
            "sandbox/java/lang/DJVMBootstrapClassAction",
            SANDBOX_CLASS_NAME,
            SANDBOX_CLASSLOADER_NAME,
            "sandbox/java/lang/DJVMConstructorAction",
            "sandbox/java/lang/DJVMDeclaredAnnotationsByTypeAction",
            "sandbox/java/lang/DJVMEnumAction",
            DJVM_EXCEPTION_NAME,
            "sandbox/java/lang/DJVMNoResource",
            "sandbox/java/lang/DJVMResourceKey",
            "sandbox/java/lang/DJVMSystemResourceAction",
            "sandbox/java/lang/DJVMThrowableWrapper",
            SANDBOX_OBJECT_NAME,
            "sandbox/java/lang/String\$InitAction",
            "sandbox/java/lang/reflect/AccessibleObject",
            "sandbox/java/lang/reflect/Constructor",
            "sandbox/java/lang/reflect/DJVM",
            "sandbox/java/lang/reflect/Executable",
            "sandbox/java/lang/reflect/Field",
            "sandbox/java/lang/reflect/Method",
            "sandbox/java/lang/reflect/Parameter",
            "sandbox/java/nio/charset/Charset\$ExtendedProviderHolder",
            "sandbox/java/security/DJVM",
            "sandbox/java/security/DJVM\$PrivilegedExceptionTask",
            "sandbox/java/security/DJVM\$PrivilegedTask",
            "sandbox/java/time/DJVM",
            "sandbox/java/time/DJVM\$InitAction",
            "sandbox/java/time/zone/ZoneRulesProvider\$1",
            "sandbox/java/util/Currency\$1",
            "sandbox/java/util/concurrent/ConcurrentHashMap\$BaseEnumerator",
            "sandbox/java/util/concurrent/atomic/AtomicIntegerFieldUpdater\$AtomicIntegerFieldUpdaterImpl",
            "sandbox/java/util/concurrent/atomic/AtomicLongFieldUpdater\$AtomicLongFieldUpdaterImpl",
            "sandbox/java/util/concurrent/atomic/AtomicReferenceFieldUpdater\$AtomicReferenceFieldUpdaterImpl",
            "sandbox/java/util/concurrent/atomic/DJVM",
            "sandbox/java/util/concurrent/locks/DJVMConditionObject",
            "sandbox/javax/security/auth/x500/DJVM",
            "sandbox/sun/misc/JavaLangAccess",
            SHARED_SECRETS,
            "$SHARED_SECRETS\$1",
            "$SHARED_SECRETS\$JavaLangAccessImpl",
            "sandbox/sun/misc/VM",
            "sandbox/sun/security/action/GetBooleanAction",
            "sandbox/sun/security/action/GetPropertyAction",
            "sandbox/sun/security/provider/ByteArrayAccess",
            "$X500_NAME\$1",
            "sandbox/sun/util/calendar/ZoneInfoFile\$1"
        ))

        /**
         * These classes are immutable despite containing static fields, and so do not
         * need to be reinitialised when the [net.corda.djvm.SandboxRuntimeContext] is
         * reset.
         */
        private val IMMUTABLE_CLASSES: Set<String> = unmodifiableSet(setOf(
            ByteOrder::class.java
        ).sandboxed() + setOf(
            "sandbox/java/nio/Bits"
        ))

        /**
         * These annotations don't need synthetic friends
         * because they have no data fields that need to
         * be transformed.
         */
        private val JVM_ANNOTATION_DESC: Set<String>

        @JvmField
        val JVM_ANNOTATIONS: Set<String>

        init {
            val simpleAnnotations = setOf(
                /**
                 * These annotations only target "Types", such as
                 * interfaces or annotations. We would need to modify
                 * [net.corda.djvm.code.impl.SandboxClassRemapper]
                 * if any annotation here could also be applied to
                 * methods, method parameters or to fields.
                 */
                java.lang.FunctionalInterface::class.java,
                java.lang.annotation.Documented::class.java,
                java.lang.annotation.Inherited::class.java,
                MustBeDocumented::class.java
            )
            JVM_ANNOTATION_DESC = unmodifiableSet(simpleAnnotations.mapTo(LinkedHashSet(), ::toDescriptor))
            JVM_ANNOTATIONS = unmodifiableSet(simpleAnnotations.sandboxed())
        }

        /**
         * These exceptions are thrown by the JVM itself, and
         * so we need to handle them without wrapping them.
         *
         * Note that this set is closed, i.e. every one
         * of these exceptions' [Throwable] super classes
         * is also within this set.
         *
         * The full list of exceptions is determined by:
         * hotspot/src/share/vm/classfile/vmSymbols.hpp
         */
        @JvmField
        val JVM_EXCEPTIONS: Set<String> = unmodifiable(setOf(
            java.io.IOException::class.java,
            java.lang.AbstractMethodError::class.java,
            java.lang.ArithmeticException::class.java,
            java.lang.ArrayIndexOutOfBoundsException::class.java,
            java.lang.ArrayStoreException::class.java,
            java.lang.ClassCastException::class.java,
            java.lang.ClassCircularityError::class.java,
            java.lang.ClassFormatError::class.java,
            java.lang.ClassNotFoundException::class.java,
            java.lang.CloneNotSupportedException::class.java,
            java.lang.Error::class.java,
            java.lang.Exception::class.java,
            java.lang.ExceptionInInitializerError::class.java,
            java.lang.IllegalAccessError::class.java,
            java.lang.IllegalAccessException::class.java,
            java.lang.IllegalArgumentException::class.java,
            java.lang.IllegalStateException::class.java,
            java.lang.IncompatibleClassChangeError::class.java,
            java.lang.IndexOutOfBoundsException::class.java,
            java.lang.InstantiationError::class.java,
            java.lang.InstantiationException::class.java,
            java.lang.InternalError::class.java,
            java.lang.LinkageError::class.java,
            java.lang.NegativeArraySizeException::class.java,
            java.lang.NoClassDefFoundError::class.java,
            java.lang.NoSuchFieldError::class.java,
            java.lang.NoSuchFieldException::class.java,
            java.lang.NoSuchMethodError::class.java,
            java.lang.NoSuchMethodException::class.java,
            java.lang.NullPointerException::class.java,
            java.lang.OutOfMemoryError::class.java,
            java.lang.ReflectiveOperationException::class.java,
            java.lang.RuntimeException::class.java,
            java.lang.StackOverflowError::class.java,
            java.lang.StringIndexOutOfBoundsException::class.java,
            java.lang.ThreadDeath::class.java,
            java.lang.Throwable::class.java,
            java.lang.UnknownError::class.java,
            java.lang.UnsatisfiedLinkError::class.java,
            java.lang.UnsupportedClassVersionError::class.java,
            java.lang.UnsupportedOperationException::class.java,
            java.lang.VerifyError::class.java,
            java.lang.VirtualMachineError::class.java,
            java.lang.reflect.InvocationTargetException::class.java,
            java.security.PrivilegedActionException::class.java
        ).sandboxed() + setOf(
            // Mentioned here to prevent the DJVM from generating a synthetic wrapper.
            "sandbox/java/lang/DJVMThrowableWrapper"
        ))

        /**
         * These interfaces will be modified as follows when
         * added to the sandbox:
         *
         * <code>interface sandbox.A extends A</code>
         *
         * Some of these interface methods will need to have synthetic
         * bridge methods stitched in too.
         *
         * THIS IS ALL FOR THE BENEFIT OF [sandbox.java.lang.String]!!
         */
        private val STITCHED_INTERFACES: Map<String, List<Member>> = unmodifiable(
            generateInterfaceBridgeMethods().mapByClassName() + mapOf(
                sandboxed(Comparable::class.java) to emptyList(),
                sandboxed(Comparator::class.java) to emptyList(),
                sandboxed(Iterator::class.java) to emptyList()
            )
        )

        /**
         * These classes have methods replaced or extra ones added when mapped into the sandbox.
         * THIS IS FOR THE BENEFIT OF [sandbox.java.lang.Enum] AND [sandbox.java.nio.charset.Charset].
         *
         * The Java Security mechanisms also require some careful surgery to prevent them from
         * trying to invoke [Unsafe][sun.misc.Unsafe] and other assorted native methods.
         */
        private val STITCHED_CLASSES: Map<String, List<Member>> = unmodifiable((
            generateJavaAnnotationMethods() +
            generateJavaCalendarMethods() +
            generateJavaTimeMethods() +
            generateJavaResourceBundleMethods() +
            generateJavaUuidMethods() +
            generateJavaPackageMethods() +
            generateJavaBitsMethods() +
            generateJavaMathMethods() +
            generateJavaEnumMethods() +
            generateJavaBaseMethods() +

            listOf(
                deleteClassInitializerFor(Modifier::class.java),
                deleteClassInitializerFor(Random::class.java),
                deleteClassInitializerFor(SecurityManager::class.java),
                deleteClassInitializerFor(CopyOnWriteArrayList::class.java)
            )
        ).mapByClassName())

        fun sandboxed(clazz: Class<*>): String = (SANDBOX_PREFIX + Type.getInternalName(clazz)).intern()
        fun Set<Class<*>>.sandboxed(): Set<String> = mapTo(LinkedHashSet(), Companion::sandboxed)

        private fun toDescriptor(clazz: Class<*>): String = "L${Type.getInternalName(clazz)};".intern()

        private fun Set<String>.merge(extra: Collection<Class<out Annotation>>): Set<String> {
            return merge(extra, ::toDescriptor)
        }

        private fun Set<String>.merge(extra: Collection<Class<out Annotation>>, mapping: (Class<*>) -> String): Set<String> {
            return if (extra.isEmpty()) {
                this
            } else {
                unmodifiable(this + extra.map(mapping))
            }
        }

        private fun Iterable<Member>.mapByClassName(): Map<String, List<Member>>
                      = groupBy(Member::className).mapValues(Map.Entry<String, List<Member>>::value)
        private fun <T> unmodifiable(items: Set<T>): Set<T> {
            return if (items.isEmpty()) emptySet() else unmodifiableSet(items)
        }
        private fun <K, V> unmodifiable(entry: Map.Entry<K, List<V>>): List<V> {
            return if (entry.value.isEmpty()) emptyList() else unmodifiableList(entry.value)
        }
        private fun <K,V> unmodifiable(items: Map<K, List<V>>): Map<K, List<V>> {
            return if (items.isEmpty()) emptyMap() else unmodifiableMap(items.mapValues(::unmodifiable))
        }

        private fun deleteClassInitializerFor(classType: Class<*>) = Member(
            access = ACC_STATIC,
            className = sandboxed(classType),
            memberName = CLASS_CONSTRUCTOR_NAME,
            descriptor = "()V",
            genericsDetails = ""
        )

        /**
         * @see [AnalysisConfiguration]
         */
        @JvmOverloads
        @JvmStatic
        fun createRoot(
            userSource: UserSource,
            visibleAnnotations: Set<Class<out Annotation>> = emptySet(),
            minimumSeverityLevel: Severity = Severity.WARNING,
            bootstrapSource: ApiSource? = null,
            overrideClasses: Set<String> = emptySet(),
            analyzeAnnotations: Boolean = false,
            prefixFilters: List<String> = emptyList(),
            classModule: ClassModule = ClassModule(),
            memberModule: MemberModule = MemberModule()
        ): AnalysisConfiguration {
            /**
             * We may need to whitelist the descriptors for methods that we
             * "stitch" into sandbox classes, to protect their invocations from
             * being remapped by [net.corda.djvm.code.impl.SandboxClassRemapper].
             */
            val whitelist = Whitelist.createWhitelist()
            val actualWhitelist = whitelist.addTextEntries(
                STITCHED_CLASSES
                    .flatMap(Map.Entry<String, List<Member>>::value)
                    .filter { it.body.isNotEmpty() }
                    .filter(MemberFilter(whitelist)::isWhitelistable)
                    .mapTo(LinkedHashSet(), Member::reference)
            )
            val templateClasses = TEMPLATE_CLASSES + overrideClasses.map(String::asResourcePath)
            val classResolver = ClassResolver(templateClasses, actualWhitelist, SANDBOX_PREFIX)

            return AnalysisConfiguration(
                parent = null,
                whitelist = actualWhitelist,
                stitchedAnnotations = STITCHED_ANNOTATIONS.merge(visibleAnnotations),
                minimumSeverityLevel = minimumSeverityLevel,
                supportingClassLoader = SourceClassLoaderImpl(classResolver, userSource, bootstrapSource),
                classResolver = classResolver,
                syntheticResolver = SyntheticResolver(JVM_EXCEPTIONS, JVM_ANNOTATIONS, SANDBOX_PREFIX),
                analyzeAnnotations = analyzeAnnotations,
                prefixFilters = prefixFilters,
                classModule = classModule,
                memberModule = memberModule
            )
        }
    }

    private class MemberFilter(private val whitelist: Whitelist) {
        fun isWhitelistable(member: Member): Boolean {
            val methodType = Type.getMethodType(member.descriptor)
            val argumentTypes = methodType.argumentTypes
            return argumentTypes.any(::isWhitelistable) || isWhitelistable(methodType.returnType)
        }

        private fun isWhitelistable(type: Type): Boolean {
            return (type.sort == Type.OBJECT && isWhitelistable(type.internalName))
                || (type.sort == Type.ARRAY && isWhitelistable(type.elementType.internalName))
        }

        private fun isWhitelistable(internalName: String): Boolean {
            return !internalName.startsWith(SANDBOX_PREFIX) && !whitelist.matches(internalName)
        }
    }
}
