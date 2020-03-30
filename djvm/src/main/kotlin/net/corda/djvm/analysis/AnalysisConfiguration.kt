package net.corda.djvm.analysis

import net.corda.djvm.code.CLASS_CONSTRUCTOR_NAME
import net.corda.djvm.code.CONSTRUCTOR_NAME
import net.corda.djvm.code.DJVM_EXCEPTION_NAME
import net.corda.djvm.code.DJVM_NAME
import net.corda.djvm.code.EmitterModule
import net.corda.djvm.code.RUNTIME_ACCOUNTER_NAME
import net.corda.djvm.code.asPackagePath
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
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes.ACC_ABSTRACT
import org.objectweb.asm.Opcodes.ACC_BRIDGE
import org.objectweb.asm.Opcodes.ACC_FINAL
import org.objectweb.asm.Opcodes.ACC_PRIVATE
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.Opcodes.ACC_SYNTHETIC
import org.objectweb.asm.Opcodes.IFNONNULL
import org.objectweb.asm.Type
import java.io.InputStream
import java.lang.reflect.Modifier
import java.nio.charset.Charset
import java.security.SecureRandom
import java.security.Security
import java.util.Collections.unmodifiableList
import java.util.Collections.unmodifiableMap
import java.util.Collections.unmodifiableSet
import java.util.Random
import java.util.concurrent.CopyOnWriteArrayList
import java.util.regex.Pattern
import javax.security.auth.x500.X500Principal
import javax.xml.datatype.DatatypeFactory
import kotlin.Comparator
import kotlin.collections.LinkedHashSet

/**
 * The configuration to use for an analysis.
 *
 * @property parent This configuration's parent [AnalysisConfiguration].
 * @property whitelist The whitelist of class names.
 * @property sandboxAnnotations A user-supplied set of regexps to control which annotations should be preserved
 * inside the sandbox.
 * @property allowedAnnotations Literal descriptors for annotations which should be preserved inside the sandbox.
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
    val sandboxAnnotations: Set<Pattern>,
    val allowedAnnotations: Set<String>,
    val stitchedAnnotations: Set<String>,
    val minimumSeverityLevel: Severity,
    val supportingClassLoader: SourceClassLoader,
    val classResolver: ClassResolver,
    val syntheticResolver: SyntheticResolver,
    val analyzeAnnotations: Boolean,
    val prefixFilters: List<String>,
    val classModule: ClassModule,
    val memberModule: MemberModule,
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

    fun isTemplateClass(className: String): Boolean = className in TEMPLATE_CLASSES

    fun isJvmException(className: String): Boolean = className in JVM_EXCEPTIONS
    fun isSandboxClass(className: String): Boolean = className.startsWith(SANDBOX_PREFIX)

    fun isUnmappedAnnotation(descriptor: String): Boolean = descriptor in UNMAPPED_ANNOTATIONS
    fun isMappedAnnotation(descriptor: String): Boolean
                = descriptor in allowedAnnotations || sandboxAnnotations.any { ann -> ann.matcher(descriptor).matches() }

    fun toSandboxClassName(header: ClassHeader): String {
        val sandboxName = classResolver.resolve(header.internalName)
        return if (header.isThrowable) {
            syntheticResolver.getThrowableOwnerName(sandboxName)
        } else {
            sandboxName
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
        private val sandboxOnlyAnnotations = linkedSetOf<String>()
        private val visibleAnnotations = linkedSetOf<Class<out Annotation>>()
        private var newMinimumSeverityLevel = minimumSeverityLevel

        override fun setMinimumSeverityLevel(level: Severity) {
            newMinimumSeverityLevel = level
        }

        override fun setSandboxOnlyAnnotations(annotations: Iterable<String>) {
            sandboxOnlyAnnotations.addAll(annotations)
        }

        override fun setVisibleAnnotations(annotations: Iterable<Class<out Annotation>>) {
            visibleAnnotations.addAll(annotations)
        }

        override fun build(): AnalysisConfiguration {
            return AnalysisConfiguration(
                parent = this@AnalysisConfiguration,
                whitelist = whitelist,
                sandboxAnnotations = unmodifiable(sandboxOnlyAnnotations.mapTo(LinkedHashSet(), ::toPattern) + sandboxAnnotations),
                allowedAnnotations = allowedAnnotations.merge(visibleAnnotations),
                stitchedAnnotations = stitchedAnnotations.merge(visibleAnnotations),
                classResolver = classResolver,
                syntheticResolver = syntheticResolver,
                minimumSeverityLevel = minimumSeverityLevel,
                analyzeAnnotations = analyzeAnnotations,
                prefixFilters = prefixFilters,
                classModule = classModule,
                memberModule = memberModule,
                supportingClassLoader = SourceClassLoader(classResolver, userSource, EmptyApi, supportingClassLoader),
                memberFormatter = memberFormatter
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
        private const val X500_NAME = "sandbox/sun/security/x509/X500Name"

        /**
         * These annotations are duplicated into the sandbox, such
         * that the sandboxed class is annotated with both the original
         * annotation and the transformed one.
         */
        private val STITCHED_ANNOTATIONS: Set<String> = unmodifiable(setOf(
            "Lkotlin/annotation/MustBeDocumented;",
            "Lkotlin/annotation/Repeatable;",
            "Lkotlin/Metadata;"
        ))

        /**
         * These are descriptors for the annotations that are considered
         * "safe" to preserve inside the sandbox.
         */
        private val ALLOWED_ANNOTATIONS: Set<String> = unmodifiable(setOf(
            "Ljava/lang/FunctionalInterface;",
            "Ljava/lang/annotation/Documented;",
            "Ljava/lang/annotation/Inherited;",
            "Ljava/lang/annotation/Repeatable;",
            "Lkotlin/annotation/MustBeDocumented;",
            "Lkotlin/annotation/Repeatable;",
            KOTLIN_METADATA
        ))

        /**
         * These annotations cannot be mapped into the sandbox, e.g.
         * because they have a method with an [Enum] value that the
         * JVM cannot assign.
         *
         * Not mapping an annotation leaves the original annotation
         * in place without also applying its sandboxed equivalent.
         *
         * Annotations which are neither mapped nor unmapped are deleted.
         */
        private val UNMAPPED_ANNOTATIONS: Set<String> = unmodifiable(setOf(
            /**
             * These meta-annotations configure how the JVM handles annotations,
             * and so these need to be preserved.
             */
            "Lkotlin/annotation/Retention;",
            "Lkotlin/annotation/Target;",
            "Ljava/lang/annotation/Retention;",
            "Ljava/lang/annotation/Target;"
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
            java.util.zip.Inflater::class.java,
            Any::class.java
        ).sandboxed() + setOf(
            "sandbox/BasicInput",
            "sandbox/BasicOutput",
            "sandbox/ImportTask",
            "sandbox/PredicateTask",
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
            "sandbox/java/lang/DJVMBootstrapClassAction",
            "sandbox/java/lang/DJVMConstructorAction",
            "sandbox/java/lang/DJVMEnumAction",
            "sandbox/java/lang/DJVMSystemResourceAction",
            "sandbox/java/lang/String\$InitAction",
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
                    val doStart = Label()
                    lineNumber(0, doStart)
                    pushObject(0)
                    invokeInterface(className, memberName, "()Lsandbox/java/util/Iterator;")
                    val doEnd = Label()
                    lineNumber(1, doEnd)
                    returnObject()
                    newLocal(
                        name = "this",
                        descriptor = "Lsandbox/java/lang/Iterable;",
                        // We are assuming that this interface is declared as "Iterable<T>".
                        signature = "Lsandbox/java/lang/Iterable<TT;>;",
                        start = doStart,
                        end = doEnd,
                        index = 0
                    )
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
            generateJavaCalendarMethods() +
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
                className = "sandbox/sun/util/locale/provider/JRELocaleProviderAdapter",
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
                className = X500_NAME,
                memberName = "asX500Principal",
                descriptor = "()Lsandbox/javax/security/auth/x500/X500Principal;"
            ) {
                /**
                 * Reimplement [sandbox.sun.security.x509.X500Name.asX500Principal] without reflection.
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
                className = X500_NAME,
                memberName = "asX500Name",
                descriptor = "(Lsandbox/javax/security/auth/x500/X500Principal;)Lsandbox/sun/security/x509/X500Name;"
            ) {
                /**
                 * Reimplement [sandbox.sun.security.x509.X500Name.asX500Name] without reflection.
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
                    val implementationClass = "sandbox/com/sun/org/apache/xerces/internal/jaxp/datatype/DatatypeFactoryImpl"
                    new(implementationClass)
                    duplicate()
                    invokeSpecial(implementationClass, CONSTRUCTOR_NAME, "()V")
                    returnObject()
                }
            }.withBody()
             .build()
        )).mapByClassName())

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

        private fun toPattern(str: String): Pattern {
            val builder = StringBuilder("^L")
            var i = 0
            while (i < str.length) {
                val c = str[i]
                when (c) {
                    '.' -> builder.append('/')
                    '?' -> builder.append('.')
                    '*' -> {
                        val j = i + 1
                        if (j < str.length && str[j] == '*') {
                            builder.append(".*")
                            i = j
                        } else {
                            builder.append("[^/]*")
                        }
                    }
                    '$' -> builder.append("\\$")
                    else -> builder.append(c)
                }
                ++i
            }
            return Pattern.compile(builder.append(";$").toString())
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
            whitelist: Whitelist,
            visibleAnnotations: Set<Class<out Annotation>> = emptySet(),
            sandboxOnlyAnnotations: Set<String> = emptySet(),
            minimumSeverityLevel: Severity = Severity.WARNING,
            bootstrapSource: ApiSource? = null,
            analyzeAnnotations: Boolean = false,
            prefixFilters: List<String> = emptyList(),
            classModule: ClassModule = ClassModule(),
            memberModule: MemberModule = MemberModule()
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
                    .mapTo(LinkedHashSet(), Member::reference)
            )
            val classResolver = ClassResolver(TEMPLATE_CLASSES, actualWhitelist, SANDBOX_PREFIX)

            return AnalysisConfiguration(
                parent = null,
                whitelist = actualWhitelist,
                sandboxAnnotations = unmodifiable<Pattern>(sandboxOnlyAnnotations.mapTo(LinkedHashSet(), ::toPattern)),
                allowedAnnotations = ALLOWED_ANNOTATIONS.merge(visibleAnnotations),
                stitchedAnnotations = STITCHED_ANNOTATIONS.merge(visibleAnnotations),
                minimumSeverityLevel = minimumSeverityLevel,
                supportingClassLoader = SourceClassLoader(classResolver, userSource, bootstrapSource),
                classResolver = classResolver,
                syntheticResolver = SyntheticResolver(JVM_EXCEPTIONS, SANDBOX_PREFIX),
                analyzeAnnotations = analyzeAnnotations,
                prefixFilters = prefixFilters,
                classModule = classModule,
                memberModule = memberModule,
                memberFormatter = MemberFormatter(classModule, memberModule)
            )
        }

        /**
         * @see [AnalysisConfiguration]
         */
        @Suppress("unused")
        @JvmStatic
        fun createRoot(
            userSource: UserSource,
            visibleAnnotations: Set<Class<out Annotation>>,
            sandboxOnlyAnnotations: Set<String>,
            minimumSeverityLevel: Severity,
            bootstrapSource: ApiSource?
        ): AnalysisConfiguration {
            return createRoot(
                userSource = userSource,
                whitelist = Whitelist.MINIMAL,
                visibleAnnotations = visibleAnnotations,
                sandboxOnlyAnnotations = sandboxOnlyAnnotations,
                minimumSeverityLevel = minimumSeverityLevel,
                bootstrapSource = bootstrapSource
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
