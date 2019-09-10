package net.corda.djvm.analysis

import net.corda.djvm.code.*
import net.corda.djvm.formatting.MemberFormatter
import net.corda.djvm.messages.Severity
import net.corda.djvm.references.*
import net.corda.djvm.source.ApiSource
import net.corda.djvm.source.EmptyApi
import net.corda.djvm.source.SourceClassLoader
import net.corda.djvm.source.UserSource
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import sun.security.x509.X500Name
import sun.util.locale.provider.JRELocaleProviderAdapter
import java.io.InputStream
import java.lang.reflect.Modifier
import java.nio.charset.Charset
import java.security.SecureRandom
import java.security.Security
import java.util.*
import java.util.Collections.*
import java.util.concurrent.CopyOnWriteArrayList
import javax.security.auth.x500.X500Principal
import javax.xml.datatype.DatatypeFactory
import kotlin.Comparator

/**
 * The configuration to use for an analysis.
 *
 * @property parent This configuration's parent [AnalysisConfiguration].
 * @property whitelist The whitelist of class names.
 * @property pinnedClasses Classes that have already been declared in the sandbox namespace and that should be
 * made available inside the sandboxed environment. These classes belong to the application
 * classloader and so are shared across all sandboxes.
 * @property stitchedAnnotations Descriptors for annotation classes whose unsandboxed values must be preserved.
 * @property classResolver Functionality used to resolve the qualified name and relevant information about a class.
 * @property exceptionResolver Resolves the internal names of synthetic exception classes.
 * @property minimumSeverityLevel The minimum severity level to log and report.
 * @property analyzeAnnotations Analyze annotations despite not being explicitly referenced.
 * @property prefixFilters Only record messages where the originating class name matches one of the provided prefixes.
 * If none are provided, all messages will be reported.
 * @property classModule Module for handling evolution of a class hierarchy during analysis.
 * @property memberModule Module for handling the specification and inspection of class members.
 * @property supportingClassLoader ClassLoader providing the classes to run inside the sandbox.
 */
class AnalysisConfiguration private constructor(
        val parent: AnalysisConfiguration?,
        val whitelist: Whitelist,
        val pinnedClasses: Set<String>,
        val stitchedAnnotations: Set<String>,
        val classResolver: ClassResolver,
        val exceptionResolver: ExceptionResolver,
        val minimumSeverityLevel: Severity,
        val analyzeAnnotations: Boolean,
        val prefixFilters: List<String>,
        val classModule: ClassModule,
        val memberModule: MemberModule,
        val supportingClassLoader: SourceClassLoader,
        private val memberFormatter: MemberFormatter
) {

    fun formatFor(member: MemberInformation): String = memberFormatter.format(member)

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

    /**
     * Creates a child [AnalysisConfiguration] with this instance as its parent.
     * The child inherits the same [whitelist] and [pinnedClasses].
     */
    fun createChild(
        userSource: UserSource,
        newMinimumSeverityLevel: Severity?,
        visibleAnnotations: Set<Class<out Annotation>>
    ): AnalysisConfiguration {
        return AnalysisConfiguration(
            parent = this,
            whitelist = whitelist,
            pinnedClasses = pinnedClasses,
            stitchedAnnotations = stitchedAnnotations.merge(visibleAnnotations),
            classResolver = classResolver,
            exceptionResolver = exceptionResolver,
            minimumSeverityLevel = newMinimumSeverityLevel ?: minimumSeverityLevel,
            analyzeAnnotations = analyzeAnnotations,
            prefixFilters = prefixFilters,
            classModule = classModule,
            memberModule = memberModule,
            supportingClassLoader = SourceClassLoader(classResolver, userSource, EmptyApi, supportingClassLoader),
            memberFormatter = memberFormatter
        )
    }

    fun isTemplateClass(className: String): Boolean = className in TEMPLATE_CLASSES
    fun isPinnedClass(className: String): Boolean = className in pinnedClasses

    fun isJvmException(className: String): Boolean = className in JVM_EXCEPTIONS
    fun isSandboxClass(className: String): Boolean = className.startsWith(SANDBOX_PREFIX) && !isPinnedClass(className)

    fun toSandboxClassName(type: Class<*>): String {
        val sandboxName = classResolver.resolve(Type.getInternalName(type))
        return if (Throwable::class.java.isAssignableFrom(type)) {
            exceptionResolver.getThrowableOwnerName(sandboxName)
        } else {
            sandboxName
        }
    }

    companion object {
        /**
         * The package name prefix to use for classes loaded into a sandbox.
         */
        const val SANDBOX_PREFIX: String = "sandbox/"

        /**
         * These meta-annotations configure how the JVM handles annotations,
         * and these need to be preserved. Currently handling Kotlin's "magic"
         * [Metadata] annotation by default.
         */
        private val STITCHED_ANNOTATIONS: Set<String> = unmodifiable(setOf(
            "Lsandbox/kotlin/Metadata;"
        ))

        /**
         * These annotations cannot be mapped into the sandbox, e.g.
         * because they have a method with an [Enum] value that the
         * JVM cannot assign.
         *
         * Not mapping an annotation leaves the original annotation
         * in place without also applying its sandboxed equivalent.
         */
        private val UNMAPPED_ANNOTATIONS: Set<String> = unmodifiable(setOf(
            "Lkotlin/annotation/Retention;",
            "Lkotlin/annotation/Target;",
            "Ljava/lang/annotation/Retention;",
            "Ljava/lang/annotation/Target;"
        ))

        fun isUnmappedAnnotation(descriptor: String): Boolean {
            return descriptor in UNMAPPED_ANNOTATIONS
        }

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
            java.util.concurrent.ConcurrentHashMap::class.java,
            java.util.concurrent.ConcurrentHashMap.KeySetView::class.java,
            java.util.concurrent.atomic.AtomicInteger::class.java,
            java.util.concurrent.atomic.AtomicLong::class.java,
            java.util.concurrent.atomic.AtomicReference::class.java,
            java.util.concurrent.locks.ReentrantLock::class.java,
            java.util.zip.CRC32::class.java,
            java.util.zip.Inflater::class.java,
            Any::class.java,
            sun.misc.JavaLangAccess::class.java,
            sun.misc.SharedSecrets::class.java,
            sun.misc.VM::class.java,
            sun.security.action.GetBooleanAction::class.java,
            sun.security.action.GetPropertyAction::class.java
        ).sandboxed() + setOf(
            "sandbox/BasicInput",
            "sandbox/BasicOutput",
            "sandbox/ImportTask",
            "sandbox/RawTask",
            "sandbox/Task",
            "sandbox/TaskTypes",
            RUNTIME_ACCOUNTER_NAME,
            "sandbox/java/io/DJVMInputStream",
            "sandbox/java/lang/Character\$Cache",
            DJVM_NAME,
            DJVM_EXCEPTION_NAME,
            "sandbox/java/lang/DJVMNoResource",
            "sandbox/java/lang/DJVMResourceKey",
            "sandbox/java/lang/DJVMThrowableWrapper",
            "sandbox/java/nio/charset/Charset\$ExtendedProviderHolder",
            "sandbox/java/time/DJVM",
            "sandbox/java/time/zone/ZoneRulesProvider\$1",
            "sandbox/java/util/Currency\$1",
            "sandbox/java/util/concurrent/ConcurrentHashMap\$BaseEnumerator",
            "sandbox/java/util/concurrent/atomic/AtomicIntegerFieldUpdater\$AtomicIntegerFieldUpdaterImpl",
            "sandbox/java/util/concurrent/atomic/AtomicLongFieldUpdater\$AtomicLongFieldUpdaterImpl",
            "sandbox/java/util/concurrent/atomic/AtomicReferenceFieldUpdater\$AtomicReferenceFieldUpdaterImpl",
            "sandbox/java/util/concurrent/atomic/DJVM",
            "sandbox/java/util/concurrent/locks/DJVMConditionObject",
            "sandbox/javax/security/auth/x500/DJVM",
            "sandbox/sun/misc/SharedSecrets\$1",
            "sandbox/sun/misc/SharedSecrets\$JavaLangAccessImpl",
            "sandbox/sun/security/provider/ByteArrayAccess",
            "sandbox/sun/security/x509/X500Name\$1",
            "sandbox/sun/util/calendar/ZoneInfoFile\$1"
        ))

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
            java.lang.VirtualMachineError::class.java
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
        private val STITCHED_INTERFACES: Map<String, List<Member>> = unmodifiable(listOf(
            object : MethodBuilder(
                access = ACC_PUBLIC or ACC_SYNTHETIC or ACC_BRIDGE,
                className = sandboxed(CharSequence::class.java),
                memberName = "subSequence",
                descriptor = "(II)Ljava/lang/CharSequence;"
            ) {
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    pushObject(0)
                    pushInteger(1)
                    pushInteger(2)
                    invokeInterface(className, memberName, "(II)L$className;")
                    returnObject()
                }
            }.withBody()
             .build(),

            MethodBuilder(
                access = ACC_PUBLIC or ACC_ABSTRACT,
                className = sandboxed(CharSequence::class.java),
                memberName = "toString",
                descriptor = "()Ljava/lang/String;"
            ).build(),

            object : MethodBuilder(
                access = ACC_PUBLIC or ACC_SYNTHETIC or ACC_BRIDGE,
                className = sandboxed(Iterable::class.java),
                memberName = "iterator",
                descriptor = "()Ljava/util/Iterator;"
            ) {
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    pushObject(0)
                    invokeInterface(className, memberName, "()Lsandbox/java/util/Iterator;")
                    returnObject()
                }
            }.withBody()
             .build()
        ).mapByClassName() + mapOf(
            sandboxed(Comparable::class.java) to emptyList(),
            sandboxed(Comparator::class.java) to emptyList(),
            sandboxed(Iterator::class.java) to emptyList()
        ))

        /**
         * These classes have methods replaced or extra ones added when mapped into the sandbox.
         * THIS IS FOR THE BENEFIT OF [sandbox.java.lang.Enum] AND [sandbox.java.nio.charset.Charset].
         *
         * The Java Security mechanisms also require some careful surgery to prevent them from
         * trying to invoke [sun.misc.Unsafe] and other assorted native methods.
         */
        private val STITCHED_CLASSES: Map<String, List<Member>> = unmodifiable((
            generateJavaTimeMethods() +
            generateJavaResourceBundleMethods() +
            generateJavaUuidMethods() +
            generateJavaPackageMethods() +
            generateJavaBitsMethods() +

            object : FromDJVMBuilder(
                className = sandboxed(Enum::class.java),
                bridgeDescriptor = "()Ljava/lang/Enum;",
                signature = "()Ljava/lang/Enum<*>;"
            ) {
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    pushObject(0)
                    invokeStatic(DJVM_NAME, "fromDJVMEnum", "(Lsandbox/java/lang/Enum;)Ljava/lang/Enum;")
                    returnObject()
                }
            }.build() +

        listOf(
            deleteClassInitializerFor(Modifier::class.java),
            deleteClassInitializerFor(Random::class.java),
            deleteClassInitializerFor(SecurityManager::class.java),
            deleteClassInitializerFor(CopyOnWriteArrayList::class.java),

            object : MethodBuilder(
                access = ACC_STATIC or ACC_PRIVATE,
                className = sandboxed(Charset::class.java),
                memberName = "providers",
                descriptor = "()Lsandbox/java/util/Iterator;",
                signature = "()Lsandbox/java/util/Iterator<Lsandbox/java/nio/charset/spi/CharsetProvider;>;"
            ) {
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    invokeStatic("sandbox/java/util/Collections", "emptyIterator", descriptor)
                    returnObject()
                }
            }.withBody()
             .build(),

            object : MethodBuilder(
                access = ACC_STATIC or ACC_PRIVATE,
                className = sandboxed(Security::class.java),
                memberName = "initialize",
                descriptor = "()V"
            ) {
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    invokeStatic(DJVM_NAME, "getSecurityProviders", "()Lsandbox/java/util/Properties;")
                    putStatic(className, "props", "Lsandbox/java/util/Properties;")
                    returnVoid()
                }
            }.withBody()
             .build(),

            object : MethodBuilder(
                access = ACC_PRIVATE or ACC_STATIC,
                className = sandboxed(SecureRandom::class.java),
                memberName = "getPrngAlgorithm",
                descriptor = "()Lsandbox/java/lang/String;"
            ) {
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    pushNull()
                    returnObject()
                }
            }.withBody()
             .build(),

            object : MethodBuilder(
                access = ACC_PRIVATE or ACC_STATIC,
                className = sandboxed(JRELocaleProviderAdapter::class.java),
                memberName = "isNonENLangSupported",
                descriptor = "()Z"
            ) {
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    pushFalse()
                    returnInteger()
                }
            }.withBody()
             .build(),

            // Create factory function to wrap java.io.InputStream.
            object : MethodBuilder(
                access = ACC_PUBLIC or ACC_STATIC,
                className = sandboxed(InputStream::class.java),
                memberName = "toDJVM",
                descriptor = "(Ljava/io/InputStream;)Lsandbox/java/io/InputStream;"
            ) {
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    val doWrap = Label()
                    lineNumber(1)
                    pushObject(0)
                    jump(IFNONNULL, doWrap)
                    pushNull()
                    returnObject()

                    lineNumber(2, doWrap)
                    new("sandbox/java/io/DJVMInputStream")
                    duplicate()
                    pushObject(0)
                    invokeSpecial("sandbox/java/io/DJVMInputStream", CONSTRUCTOR_NAME, "(Ljava/io/InputStream;)V")
                    returnObject()
                }
            }.withBody()
             .build(),

            /**
             * Create [sandbox.javax.security.auth.x500.X500Principal.unwrap] method
             * to expose existing private [X500Principal.thisX500Name] field.
             */
            object : MethodBuilder(
                access = ACC_FINAL,
                className = sandboxed(X500Principal::class.java),
                memberName = "unwrap",
                descriptor = "()Lsandbox/sun/security/x509/X500Name;"
            ) {
                /**
                 * Implement package private accessor.
                 *     return thisX500Name
                 */
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    pushObject(0)
                    pushField(className, "thisX500Name", "Lsandbox/sun/security/x509/X500Name;")
                    returnObject()
                }
            }.withBody()
             .build(),

            /**
             * Reimplement these methods so that they don't require reflection.
             */
            object : MethodBuilder(
                access = ACC_PUBLIC,
                className = sandboxed(X500Name::class.java),
                memberName = "asX500Principal",
                descriptor = "()Lsandbox/javax/security/auth/x500/X500Principal;"
            ) {
                /**
                 * Reimplement [X500Name.asX500Principal] without reflection.
                 *     return DJVM.create(this)
                 */
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    pushObject(0)
                    invokeStatic(
                        owner = "sandbox/javax/security/auth/x500/DJVM",
                        name = "create",
                        descriptor = "(Lsandbox/sun/security/x509/X500Name;)Lsandbox/javax/security/auth/x500/X500Principal;"
                    )
                    returnObject()
                }
            }.withBody()
             .build(),
            object : MethodBuilder(
                access = ACC_PUBLIC or ACC_STATIC,
                className = sandboxed(X500Name::class.java),
                memberName = "asX500Name",
                descriptor = "(Lsandbox/javax/security/auth/x500/X500Principal;)Lsandbox/sun/security/x509/X500Name;"
            ) {
                /**
                 * Reimplement [X500Name.asX500Name] without reflection.
                 *     X500Name name = DJVM.unwrap(principal)
                 *     name.x500Principal = principal
                 *     return name
                 */
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    pushObject(0)
                    invokeStatic(
                        owner = "sandbox/javax/security/auth/x500/DJVM",
                        name = "unwrap",
                        descriptor = "(Lsandbox/javax/security/auth/x500/X500Principal;)Lsandbox/sun/security/x509/X500Name;"
                    )
                    popObject(1)
                    pushObject(1)
                    pushObject(0)
                    popField(
                        owner = className,
                        name = "x500Principal",
                        descriptor = "Lsandbox/javax/security/auth/x500/X500Principal;"
                    )
                    pushObject(1)
                    returnObject()
                }
            }.withBody()
             .build(),

            object : MethodBuilder(
                access = ACC_PUBLIC or ACC_STATIC,
                className = sandboxed(DatatypeFactory::class.java),
                memberName = "newInstance",
                descriptor = "()Lsandbox/javax/xml/datatype/DatatypeFactory;"
            ) {
                /**
                 * Reimplement [DatatypeFactory.newInstance] to use the JDK's basic implementation.
                 *     return new com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl()
                 */
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    val implementationClass = sandboxed(com.sun.org.apache.xerces.internal.jaxp.datatype.DatatypeFactoryImpl::class.java)
                    new(implementationClass)
                    duplicate()
                    invokeSpecial(implementationClass, CONSTRUCTOR_NAME, "()V")
                    returnObject()
                }
            }.withBody()
             .build()
        )).mapByClassName())

        fun sandboxed(clazz: Class<*>): String = (SANDBOX_PREFIX + Type.getInternalName(clazz)).intern()
        fun Set<Class<*>>.sandboxed(): Set<String> = map(Companion::sandboxed).toSet()

        private fun sandboxDescriptor(clazz: Class<*>): String = "L$SANDBOX_PREFIX${Type.getInternalName(clazz)};"

        private fun Set<String>.merge(extra: Collection<Class<out Annotation>>): Set<String> {
            return if (extra.isEmpty()) {
                this
            } else {
                unmodifiable(this + extra.map(::sandboxDescriptor))
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
        fun createRoot(
            userSource: UserSource,
            whitelist: Whitelist,
            pinnedClasses: Set<String> = emptySet(),
            visibleAnnotations: Set<Class<out Annotation>> = emptySet(),
            minimumSeverityLevel: Severity = Severity.WARNING,
            analyzeAnnotations: Boolean = false,
            prefixFilters: List<String> = emptyList(),
            classModule: ClassModule = ClassModule(),
            memberModule: MemberModule = MemberModule(),
            bootstrapSource: ApiSource? = null
        ): AnalysisConfiguration {
            /**
             * We may need to whitelist the descriptors for methods that we
             * "stitch" into sandbox classes, to protect their invocations from
             * being remapped by [net.corda.djvm.rewiring.SandboxClassRemapper].
             */
            val actualWhitelist = whitelist.addTextEntries(
                STITCHED_CLASSES
                    .flatMap(Map.Entry<String, List<Member>>::value)
                    .filter { it.body.isNotEmpty() }
                    .filter(MemberFilter(whitelist)::isWhitelistable)
                    .map(Member::reference)
                    .toSet()
            )
            val actualPinnedClasses = unmodifiable(pinnedClasses)
            val classResolver = ClassResolver(actualPinnedClasses, TEMPLATE_CLASSES, actualWhitelist, SANDBOX_PREFIX)

            return AnalysisConfiguration(
                parent = null,
                whitelist = actualWhitelist,
                pinnedClasses = actualPinnedClasses,
                stitchedAnnotations = STITCHED_ANNOTATIONS.merge(visibleAnnotations),
                classResolver = classResolver,
                exceptionResolver = ExceptionResolver(JVM_EXCEPTIONS, actualPinnedClasses, SANDBOX_PREFIX),
                minimumSeverityLevel = minimumSeverityLevel,
                analyzeAnnotations = analyzeAnnotations,
                prefixFilters = prefixFilters,
                classModule = classModule,
                memberModule = memberModule,
                supportingClassLoader = SourceClassLoader(classResolver, userSource, bootstrapSource),
                memberFormatter = MemberFormatter(classModule, memberModule)
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
