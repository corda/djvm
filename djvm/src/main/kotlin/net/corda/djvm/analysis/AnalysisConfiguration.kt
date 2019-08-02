package net.corda.djvm.analysis

import net.corda.djvm.code.DJVM_NAME
import net.corda.djvm.code.CONSTRUCTOR_NAME
import net.corda.djvm.code.EmitterModule
import net.corda.djvm.code.RUNTIME_ACCOUNTER_NAME
import net.corda.djvm.code.djvmException
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
import javax.security.auth.x500.X500Principal
import javax.xml.datatype.DatatypeFactory
import kotlin.Comparator

/**
 * The configuration to use for an analysis.
 *
 * @property whitelist The whitelist of class names.
 * @property pinnedClasses Classes that have already been declared in the sandbox namespace and that should be
 * made available inside the sandboxed environment. These classes belong to the application
 * classloader and so are shared across all sandboxes.
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
        val whitelist: Whitelist,
        val pinnedClasses: Set<String>,
        val classResolver: ClassResolver,
        val exceptionResolver: ExceptionResolver,
        val minimumSeverityLevel: Severity,
        val analyzeAnnotations: Boolean,
        val prefixFilters: List<String>,
        val classModule: ClassModule,
        val memberModule: MemberModule,
        val supportingClassLoader: SourceClassLoader
) : AutoCloseable {
    private val memberFormatter = MemberFormatter(classModule, memberModule)

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

    @Throws(Exception::class)
    override fun close() {
        supportingClassLoader.close()
    }

    /**
     * Creates a child [AnalysisConfiguration] with this instance as its parent.
     * The child inherits the same [whitelist] and [pinnedClasses].
     */
    fun createChild(
        userSource: UserSource,
        newMinimumSeverityLevel: Severity?
    ): AnalysisConfiguration {
        return AnalysisConfiguration(
            whitelist = whitelist,
            pinnedClasses = pinnedClasses,
            classResolver = classResolver,
            exceptionResolver = exceptionResolver,
            minimumSeverityLevel = newMinimumSeverityLevel ?: minimumSeverityLevel,
            analyzeAnnotations = analyzeAnnotations,
            prefixFilters = prefixFilters,
            classModule = classModule,
            memberModule = memberModule,
            supportingClassLoader = SourceClassLoader(classResolver, userSource, EmptyApi)
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
            java.lang.Number::class.java,
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
            java.util.concurrent.locks.ReentrantLock::class.java,
            kotlin.Any::class.java,
            sun.misc.JavaLangAccess::class.java,
            sun.misc.SharedSecrets::class.java,
            sun.misc.VM::class.java,
            sun.security.action.GetBooleanAction::class.java,
            sun.security.action.GetPropertyAction::class.java
        ).sandboxed() + setOf(
            "sandbox/RawTask",
            "sandbox/Task",
            "sandbox/TaskTypes",
            RUNTIME_ACCOUNTER_NAME,
            "sandbox/java/io/DJVMInputStream",
            "sandbox/java/lang/Character\$Cache",
            DJVM_NAME,
            djvmException,
            "sandbox/java/lang/DJVMNoResource",
            "sandbox/java/lang/DJVMResourceKey",
            "sandbox/java/lang/DJVMThrowableWrapper",
            "sandbox/java/nio/charset/Charset\$ExtendedProviderHolder",
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
            "sandbox/sun/security/x509/X500Name\$1"
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
            ).build()
        ).mapByClassName() + listOf(
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

        private const val GET_BUNDLE = "getBundle"

        /**
         * These classes have methods replaced or extra ones added when mapped into the sandbox.
         * THIS IS FOR THE BENEFIT OF [sandbox.java.lang.Enum] AND [sandbox.java.nio.charset.Charset].
         *
         * The Java Security mechanisms also require some careful surgery to prevent them from
         * trying to invoke [sun.misc.Unsafe] and other assorted native methods.
         */
        private val STITCHED_CLASSES: Map<String, List<Member>> = unmodifiable(listOf(
            object : MethodBuilder(
                access = ACC_FINAL or ACC_PROTECTED,
                className = sandboxed(Enum::class.java),
                memberName = "fromDJVM",
                descriptor = "()Ljava/lang/Enum;",
                signature = "()Ljava/lang/Enum<*>;"
            ) {
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    pushObject(0)
                    invokeStatic(DJVM_NAME, "fromDJVMEnum", "(Lsandbox/java/lang/Enum;)Ljava/lang/Enum;")
                    returnObject()
                }
            }.withBody()
             .build(),

            object : MethodBuilder(
                access = ACC_BRIDGE or ACC_SYNTHETIC or ACC_PROTECTED,
                className = sandboxed(Enum::class.java),
                memberName = "fromDJVM",
                descriptor = "()Ljava/lang/Object;"
            ) {
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    pushObject(0)
                    invokeVirtual(className, memberName, "()Ljava/lang/Enum;")
                    returnObject()
                }
            }.withBody()
             .build()
        ).mapByClassName() + listOf(
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
             .build()
        ).mapByClassName() + listOf(
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
             .build()
        ).mapByClassName() + listOf(
            deleteClassInitialiserFor(Modifier::class.java)
        ).mapByClassName() + listOf(
            deleteClassInitialiserFor(Random::class.java)
        ).mapByClassName() + listOf(
            deleteClassInitialiserFor(SecurityManager::class.java)
        ).mapByClassName() + listOf(
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
             .build()
        ).mapByClassName() + listOf(
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
             .build()
        ).mapByClassName() + listOf(
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
             .build()
        ).mapByClassName() + listOf(
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
             .build()
        ).mapByClassName() + listOf(
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
             .build()
        ).mapByClassName() + listOf(
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
        ).mapByClassName() + listOf(
            /**
             * Redirect the [ResourceBundle] handling.
             */
            object : MethodBuilder(
                access = ACC_PUBLIC or ACC_STATIC,
                className = sandboxed(ResourceBundle::class.java),
                memberName = GET_BUNDLE,
                descriptor = "(Lsandbox/java/lang/String;)Lsandbox/java/util/ResourceBundle;"
            ) {
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    pushObject(0)
                    pushDefaultLocale()
                    pushDefaultControl()
                    returnResourceBundle()
                }
            }.withBody()
             .build(),
            object : MethodBuilder(
                access = ACC_PUBLIC or ACC_STATIC,
                className = sandboxed(ResourceBundle::class.java),
                memberName = GET_BUNDLE,
                descriptor = "(Lsandbox/java/lang/String;Lsandbox/java/util/ResourceBundle\$Control;)Lsandbox/java/util/ResourceBundle;"
            ) {
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    pushObject(0)
                    pushDefaultLocale()
                    pushObject(1)
                    returnResourceBundle()
                }
            }.withBody()
             .build(),
            object : MethodBuilder(
                access = ACC_PUBLIC or ACC_STATIC,
                className = sandboxed(ResourceBundle::class.java),
                memberName = GET_BUNDLE,
                descriptor = "(Lsandbox/java/lang/String;Lsandbox/java/util/Locale;)Lsandbox/java/util/ResourceBundle;"
            ) {
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    pushObject(0)
                    pushObject(1)
                    pushDefaultControl()
                    returnResourceBundle()
                }
            }.withBody()
             .build(),
            object : MethodBuilder(
                access = ACC_PUBLIC or ACC_STATIC,
                className = sandboxed(ResourceBundle::class.java),
                memberName = GET_BUNDLE,
                descriptor = "(Lsandbox/java/lang/String;Lsandbox/java/util/Locale;Lsandbox/java/util/ResourceBundle\$Control;)Lsandbox/java/util/ResourceBundle;"
            ) {
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    pushObject(0)
                    pushObject(1)
                    pushObject(2)
                    returnResourceBundle()
                }
            }.withBody()
             .build(),
            object : MethodBuilder(
                access = ACC_PUBLIC or ACC_STATIC,
                className = sandboxed(ResourceBundle::class.java),
                memberName = GET_BUNDLE,
                descriptor = "(Lsandbox/java/lang/String;Lsandbox/java/util/Locale;Ljava/lang/ClassLoader;)Lsandbox/java/util/ResourceBundle;"
            ) {
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    pushObject(0)
                    pushObject(1)
                    pushDefaultControl()
                    returnResourceBundle()
                }
            }.withBody()
             .build(),
            object : MethodBuilder(
                access = ACC_PUBLIC or ACC_STATIC,
                className = sandboxed(ResourceBundle::class.java),
                memberName = GET_BUNDLE,
                descriptor = "(Lsandbox/java/lang/String;Lsandbox/java/util/Locale;Ljava/lang/ClassLoader;Lsandbox/java/util/ResourceBundle\$Control;)Lsandbox/java/util/ResourceBundle;"
            ) {
                override fun writeBody(emitter: EmitterModule) = with(emitter) {
                    pushObject(0)
                    pushObject(1)
                    pushObject(3)
                    returnResourceBundle()
                }
            }.withBody()
             .build()
        ).mapByClassName())

        private fun sandboxed(clazz: Class<*>): String = (SANDBOX_PREFIX + Type.getInternalName(clazz)).intern()
        private fun Set<Class<*>>.sandboxed(): Set<String> = map(Companion::sandboxed).toSet()
        private fun Iterable<Member>.mapByClassName(): Map<String, List<Member>>
                      = groupBy(Member::className).mapValues(Map.Entry<String, List<Member>>::value)
        private fun <T> unmodifiable(items: Set<T>): Set<T> {
            return if (items.isEmpty()) emptySet() else unmodifiableSet(items)
        }
        private fun <K, V> unmodifiable(entry: Map.Entry<K, List<V>>): List<V> {
            return if (entry.value.isEmpty()) emptyList() else unmodifiableList(entry.value)
        }
        private fun <K,V> unmodifiable(items: Map<K, List<V>>): Map<K, List<V>> {
            return if (items.isEmpty()) emptyMap() else unmodifiableMap(items.mapValues(Companion::unmodifiable))
        }

        private fun deleteClassInitialiserFor(classType: Class<*>) = Member(
            access = ACC_STATIC,
            className = sandboxed(classType),
            memberName = "<clinit>",
            descriptor = "()V",
            genericsDetails = ""
        )

        private fun EmitterModule.returnResourceBundle() {
            invokeStatic(
                owner = DJVM_NAME,
                name = GET_BUNDLE,
                descriptor = "(Lsandbox/java/lang/String;Lsandbox/java/util/Locale;Lsandbox/java/util/ResourceBundle\$Control;)Lsandbox/java/util/ResourceBundle;"
            )
            returnObject()
        }
        private fun EmitterModule.pushDefaultLocale() {
            invokeStatic(
                owner = sandboxed(Locale::class.java),
                name = "getDefault",
                descriptor = "()Lsandbox/java/util/Locale;"
            )
        }
        private fun EmitterModule.pushDefaultControl() {
            /*
             * The baseName parameter is expected already to have been
             * pushed onto the stack, just below the Locale value, so
             * emit instructions to rearrange the stack as follows:
             *     [W1]    [W2]
             *     [W2] -> [W1]
             *             [W2]
             */
            instruction(DUP2)
            pop()
            invokeStatic(
                owner = sandboxed(ResourceBundle::class.java),
                name = "getDefaultControl",
                descriptor = "(Lsandbox/java/lang/String;)Lsandbox/java/util/ResourceBundle\$Control;"
            )
        }

        /**
         * @see [AnalysisConfiguration]
         */
        fun createRoot(
            userSource: UserSource,
            whitelist: Whitelist,
            pinnedClasses: Set<String> = emptySet(),
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
                whitelist = actualWhitelist,
                pinnedClasses = actualPinnedClasses,
                classResolver = classResolver,
                exceptionResolver = ExceptionResolver(JVM_EXCEPTIONS, actualPinnedClasses, SANDBOX_PREFIX),
                minimumSeverityLevel = minimumSeverityLevel,
                analyzeAnnotations = analyzeAnnotations,
                prefixFilters = prefixFilters,
                classModule = classModule,
                memberModule = memberModule,
                supportingClassLoader = SourceClassLoader(classResolver, userSource, bootstrapSource)
            )
        }
    }

    private open class MethodBuilder(
            protected val access: Int,
            protected val className: String,
            protected val memberName: String,
            protected val descriptor: String,
            protected val signature: String = ""
    ) {
        private val bodies = mutableListOf<MethodBody>()

        protected open fun writeBody(emitter: EmitterModule) {}

        fun withBody(): MethodBuilder {
            bodies.add(::writeBody)
            return this
        }

        fun build() = Member(
            access = access,
            className = className,
            memberName = memberName,
            descriptor = descriptor,
            genericsDetails = signature,
            body = bodies
        )
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
