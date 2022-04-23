@file:JvmName("DJVM")
@file:Suppress("unused")
package sandbox.java.lang

import net.corda.djvm.SandboxRuntimeContext
import net.corda.djvm.analysis.AnalysisConfiguration.Companion.JVM_ANNOTATIONS
import net.corda.djvm.analysis.AnalysisConfiguration.Companion.JVM_EXCEPTIONS
import net.corda.djvm.analysis.SyntheticResolver.Companion.getDJVMSynthetic
import net.corda.djvm.code.impl.CLASS_RESET_NAME
import net.corda.djvm.execution.SandboxRuntimeException
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.rules.RuleViolationError
import net.corda.djvm.rules.implementation.*
import org.objectweb.asm.Type
import sandbox.isEntryPoint
import sandbox.java.io.*
import sandbox.java.lang.annotation.Annotation
import sandbox.java.nio.ByteOrder
import sandbox.java.util.Date
import sandbox.java.util.Enumeration
import sandbox.java.util.Locale
import sandbox.java.util.MissingResourceException
import sandbox.java.util.Properties
import sandbox.java.util.ResourceBundle
import sandbox.java.util.UUID
import java.io.IOException
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodHandles.Lookup
import java.lang.invoke.MethodType
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Array.newInstance
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier.isPrivate
import java.lang.reflect.Modifier.isPublic
import java.lang.reflect.Proxy
import java.security.AccessController
import java.security.PrivilegedAction
import java.security.PrivilegedActionException
import java.security.PrivilegedExceptionAction
import java.util.function.BiConsumer

/**
 * Register this class's reset method to flush any static data.
 * We will preserve [systemClassLoader] and [sandboxThrowable]
 * because these do not change.
 */
@Suppress("FunctionName")
@JvmSynthetic
private fun `djvm$reset`(resetter: BiConsumer<Any?, kotlin.String>) {
    resetter.accept(mutableMapOf<DJVMResourceKey, ResourceBundle>(), "resourceCache")
    resetter.accept(mutableMapOf<kotlin.Throwable, Throwable>(), "sandboxedExceptions")

    /**
     * The [String] class is immutable. Push these [String] constants
     * back into the [SandboxRuntimeContext]'s cache of interned values.
     */
    String.toDJVM("").intern()
    String.valueOf(true).intern()
    String.valueOf(false).intern()
    String.NEWLINE.intern()

    /**
     * The [ByteOrder] enum is also immutable, so ensure that its
     * [String] values are also restored to the interned cache.
     */
    ByteOrder.BIG_ENDIAN.toDJVMString().intern()
    ByteOrder.LITTLE_ENDIAN.toDJVMString().intern()
}
private val registration = forReset(MethodHandles.lookup(), CLASS_RESET_NAME)

private const val SANDBOX_PREFIX = "sandbox."
private val OBJECT_ARRAY = "^(\\[++L)([^;]++);\$".toRegex()
private val PRIMITIVE_ARRAY = "^(\\[)++[IJSCBZFD]\$".toRegex()

fun Any.unsandbox(): Any {
    return when (this) {
        is Object -> fromDJVM()
        is Array<*> -> fromDJVMArray()
        is Annotation -> fail("Annotation proxies cannot escape sandbox!")
        else -> this
    }
}

@Throws(ClassNotFoundException::class)
fun Any.sandbox(): Any {
    @Suppress("RemoveRedundantQualifierName")
    return when(this) {
        is kotlin.String -> String.toDJVM(this)
        is kotlin.Char -> Character.toDJVM(this)
        is kotlin.Long -> Long.toDJVM(this)
        is kotlin.Int -> Integer.toDJVM(this)
        is kotlin.Short -> Short.toDJVM(this)
        is kotlin.Byte -> Byte.toDJVM(this)
        is kotlin.Float -> Float.toDJVM(this)
        is kotlin.Double -> Double.toDJVM(this)
        is kotlin.Boolean -> Boolean.toDJVM(this)
        is kotlin.Enum<*> -> toDJVMEnum()
        is kotlin.Throwable -> toDJVMThrowable()
        is java.math.BigInteger -> toDJVMBigInteger()
        is java.math.BigDecimal -> sandbox.java.math.BigDecimal(unscaledValue().toDJVMBigInteger(), scale())
        is java.util.Date -> Date(time)
        is java.io.InputStream -> InputStream.toDJVM(this)
        is java.util.UUID -> UUID(mostSignificantBits, leastSignificantBits)
        is java.time.Duration -> sandbox.java.time.Duration.ofSeconds(seconds, nano.toLong())
        is java.time.Instant -> sandbox.java.time.Instant.ofEpochSecond(epochSecond, nano.toLong())
        is java.time.LocalDate -> toDJVM()
        is java.time.LocalDateTime -> sandbox.java.time.LocalDateTime.of(toLocalDate().toDJVM(), toLocalTime().toDJVM())
        is java.time.LocalTime -> toDJVM()
        is java.time.MonthDay -> sandbox.java.time.MonthDay.of(monthValue, dayOfMonth)
        is java.time.OffsetDateTime -> sandbox.java.time.OffsetDateTime.of(toLocalDateTime().toDJVM(), offset.toDJVM() as sandbox.java.time.ZoneOffset)
        is java.time.OffsetTime -> sandbox.java.time.OffsetTime.of(toLocalTime().toDJVM(), offset.toDJVM() as sandbox.java.time.ZoneOffset)
        is java.time.Period -> sandbox.java.time.Period.of(years, months, days)
        is java.time.Year -> sandbox.java.time.Year.of(value)
        is java.time.YearMonth -> sandbox.java.time.YearMonth.of(year, monthValue)
        is java.time.ZonedDateTime -> sandbox.java.time.ZonedDateTime.createDJVM(
            toLocalDateTime().toDJVM(),
            offset.toDJVM() as sandbox.java.time.ZoneOffset,
            zone.toDJVM()
        )
        is java.time.ZoneId -> sandbox.java.time.ZoneId.of(String.toDJVM(id))
        is Array<*> -> toDJVMArray()
        is Member ->
            if (systemClassLoader.contains(declaringClass)) {
                when(this) {
                    is Constructor<*> -> sandbox.java.lang.reflect.DJVM.toDJVM(this)
                    is Method -> sandbox.java.lang.reflect.DJVM.toDJVM(this)
                    is Field -> sandbox.java.lang.reflect.DJVM.toDJVM(this)
                    else -> fail("Cannot sandbox $this")
                }
            } else {
                fail("Cannot sandbox $this")
            }

        /**
         * [Class] and [Constructor] are white-listed inside the sandbox,
         * which means that they're used "as is". So prevent the user from
         * passing bad instances into the sandbox through the front door!
         *
         * Objects which implement [kotlin.Annotation] are almost certainly
         * dynamic proxies, so keep them out of the sandbox too!
         */
        is Class<*>, is kotlin.Annotation -> fail("Cannot sandbox $this")
        is ClassLoader -> fail("Cannot sandbox a ClassLoader")

        // Default behaviour...
        else -> this
    }
}

fun kotlin.Throwable.toRuleViolationError(): RuleViolationError {
    return if (this is RuleViolationError) {
        this
    } else {
        RuleViolationError("${this::class.java.name} -> $message").also { t ->
            t.stackTrace = stackTrace
        }
    }
}

/**
 * Replace this [Throwable] with an instance of [RuntimeException] that
 * has the same stack trace. Primarily used by [sandbox.ImportTask] to
 * ensure that the DJVM can always handle an exception thrown from
 * outside the sandbox.
 */
fun kotlin.Throwable.toRuntimeException(): RuntimeException {
    return RuntimeException("${this::class.java.name} -> $message", cause?.toRuntimeException()).also { t ->
        t.stackTrace = stackTrace
    }
}

fun kotlin.Throwable.escapeSandbox(): kotlin.Throwable {
    val sandboxed = (this as? DJVMException)?.throwable ?: sandboxedExceptions.remove(this)
    return sandboxed?.escapeSandbox() ?: safeCopy()
}

private fun Throwable.escapeSandbox(): kotlin.Throwable {
    val escapingCause = cause?.escapeSandbox()
    val sandboxedName = javaClass.name
    return try {
        val escaping = if (Type.getInternalName(javaClass) in JVM_EXCEPTIONS) {
            val escapingMessage = String.fromDJVM(message)
            // We map these exceptions to their equivalent JVM classes.
            when {
                this is sandbox.java.lang.reflect.InvocationTargetException ->
                    InvocationTargetException::class.java.getPrivilegedConstructor(kotlin.Throwable::class.java, kotlin.String::class.java)
                        .newInstance(escapingCause, escapingMessage)
                this is ExceptionInInitializerError && escapingCause != null ->
                    java.lang.ExceptionInInitializerError::class.java.getPrivilegedConstructor(kotlin.Throwable::class.java)
                        .newInstance(escapingCause)
                this is sandbox.java.security.PrivilegedActionException ->
                    PrivilegedActionException::class.java.getPrivilegedConstructor(kotlin.Exception::class.java)
                        .newInstance(escapingCause as kotlin.Exception)
                else -> {
                    @Suppress("unchecked_cast")
                    val escapingType = loadBootstrapClass(sandboxedName.fromSandboxPackage()) as Class<out kotlin.Throwable>
                    try {
                        escapingType.getPrivilegedConstructor(kotlin.String::class.java, kotlin.Throwable::class.java)
                            .newInstance(escapingMessage, escapingCause)
                    } catch (_: NoSuchMethodException) {
                        try {
                            escapingType.getPrivilegedConstructor(kotlin.String::class.java)
                                .newInstance(escapingMessage)
                        } catch (_: NoSuchMethodException) {
                            escapingType.getPrivilegedConstructor()
                                .newInstance()
                        }.apply {
                            escapingCause?.run(::initCause)
                        }
                    }
                }
            }
        } else {
            val escapingMessage = "$sandboxedName -> $message"
            val sourceType = loadSandboxClass(getDJVMSynthetic(sandboxedName))
            when {
                RuntimeException::class.java.isAssignableFrom(sourceType) -> RuntimeException(escapingMessage)
                kotlin.Exception::class.java.isAssignableFrom(sourceType) -> kotlin.Exception(escapingMessage)
                Error::class.java.isAssignableFrom(sourceType) -> Error(escapingMessage)
                else -> Throwable(escapingMessage)
            }.apply {
                escapingCause?.run(::initCause)
            }
        }
        escaping.apply {
            stackTrace = copyFromDJVM(this@escapeSandbox.stackTrace)
            sanitise(0)
            this@escapeSandbox.suppressed.forEach { sup ->
                addSuppressed(sup.escapeSandbox())
            }
        }
    } catch (e: kotlin.Exception) {
        e.toRuleViolationError()
    }
}

/**
 * This exception is assumed to have been thrown by the JVM
 * itself, which means that there was no [Throwable] to find
 * in [sandboxedExceptions].
 */
private fun kotlin.Throwable.safeCopy(): kotlin.Throwable {
    sanitise(0)
    return when {
        /**
         * [InvocationTargetException], [java.lang.ExceptionInInitializerError]
         * and [PrivilegedActionException] can contain a sandbox exception as
         * their underlying target / cause.
         */
        this is InvocationTargetException ->
            InvocationTargetException(cause?.escapeSandbox(), message).also(::copyExtraTo)
        this is java.lang.ExceptionInInitializerError && cause != null ->
            ExceptionInInitializerError(cause?.escapeSandbox()).also(::copyExtraTo)
        this is PrivilegedActionException ->
            PrivilegedActionException(cause?.escapeSandbox() as? kotlin.Exception).also(::copyExtraTo)
        else -> this
    }
}

private fun kotlin.Throwable.copyExtraTo(t: kotlin.Throwable) {
    // This stack trace should already have been sanitised.
    t.stackTrace = stackTrace
    suppressed.forEach { sup ->
        t.addSuppressed(sup.safeCopy())
    }
}

private fun Array<*>.fromDJVMArray(): Array<*> = Object.fromDJVM(this)

private fun java.math.BigInteger.toDJVMBigInteger(): sandbox.java.math.BigInteger {
    return sandbox.java.math.BigInteger(signum(), toByteArray())
}

private fun java.time.LocalDate.toDJVM(): sandbox.java.time.LocalDate {
    return sandbox.java.time.LocalDate.of(year, monthValue, dayOfMonth)
}

private fun java.time.LocalTime.toDJVM(): sandbox.java.time.LocalTime {
    return sandbox.java.time.LocalTime.of(hour, minute, second, nano)
}

private fun java.time.LocalDateTime.toDJVM(): sandbox.java.time.LocalDateTime {
    return sandbox.java.time.LocalDateTime.of(toLocalDate().toDJVM(), toLocalTime().toDJVM())
}

private fun java.time.ZoneId.toDJVM(): sandbox.java.time.ZoneId {
    return sandbox.java.time.ZoneId.of(String.toDJVM(id))
}

/**
 * Throws a [RuleViolationError] to exit the sandbox.
 * This function never returns, and we can inform the
 * caller not to expect us to by invoking it via:
 *
 *     throw DJVM.fail("message")
 */
fun fail(message: kotlin.String): Error {
    // Discard the first stack frame so that
    // our invoker's frame is on top.
    throw RuleViolationError(message).sanitise(1)
}

fun failApi(message: kotlin.String): Error {
    // Discard the first two stack frames so that the
    // function which invoked our invoker is in top.
    throw RuleViolationError("Disallowed reference to API; $message").sanitise(2)
}

internal fun unsandbox(name: kotlin.String) = name.removePrefix(SANDBOX_PREFIX)

/**
 * Add this generated class to the list of those that will
 * need to be reset before this classloader can be reused.
 */
fun forReset(clazz: Class<*>, resetter: MethodHandle) {
    SandboxRuntimeContext.instance.addToReset(clazz, resetter)
}

/**
 * Add a hand-written class to the reset list.
 */
fun forReset(lookup: Lookup, resetMethod: kotlin.String) {
    val resetType = MethodType.methodType(Void::class.javaPrimitiveType, BiConsumer::class.java)
    val resetClass = lookup.lookupClass()
    SandboxRuntimeContext.instance.addToReset(resetClass, lookup.findStatic(resetClass, resetMethod, resetType))
}

/**
 * Use [Class.forName] so that we can also fetch classes for array types.
 * Also use the sandbox's classloader explicitly here, because this invoking
 * class might belong to a shared parent classloader.
 */
private fun loadSandboxClass(name: kotlin.String): Class<*> = Class.forName(name, false, systemClassLoader)
private fun loadBootstrapClass(name: kotlin.String): Class<*> = doPrivileged(DJVMBootstrapClassAction(name))

@Throws(ClassNotFoundException::class)
internal fun Class<*>.toDJVMType(): Class<*> = loadSandboxClass(systemClassLoader.resolveName(name))

private inline fun <reified T> Class<*>.toDJVM(): Class<out T> = toDJVMType().asSubclass(T::class.java)

@Throws(ClassNotFoundException::class)
internal fun Class<*>.fromDJVMType(): Class<*> {
    return if (isArray) {
        val componentName = name
        val idx = componentName.indexOf('L') + 1
        loadSandboxClass(componentName.substring(0, idx) + componentName.drop(idx).fromSandboxPackage())
    } else {
        loadSandboxClass(name.fromSandboxPackage())
    }
}

private fun kotlin.String.fromSandboxPackage(): kotlin.String {
    return if (startsWith(SANDBOX_PREFIX)) {
        drop(SANDBOX_PREFIX.length)
    } else {
        this
    }
}

private fun Array<*>.toDJVMArray(): Array<*> {
    @Suppress("unchecked_cast")
    return (newInstance(javaClass.componentType.toDJVMType(), size) as Array<Any?>).also {
        for ((i, item) in withIndex()) {
            it[i] = item?.sandbox()
        }
    }
}

/**
 * Replacement function for [toString][java.lang.Object.toString], because some
 * objects (i.e. arrays) cannot be replaced by [sandbox.java.lang.Object].
 */
fun toString(obj: Any?): String {
    return when {
        obj is Object -> obj.toDJVMString()
        obj is Annotation -> obj.toDJVMString()
        obj != null -> Object.toDJVMString(System.identityHashCode(obj))
        else -> // Throw the same exception that the JVM would throw in this case.
            throw NullPointerException().sanitise(1)
    }
}

/**
 * Replacement function for Object.hashCode(), because some objects
 * (i.e. arrays) cannot be replaced by [sandbox.java.lang.Object].
 * Note that the DJVM will implement [Annotation] using a dynamic
 * proxy, and so we MUST execute [hashCode] normally to invoke
 * its handler.
 */
fun hashCode(obj: Any?): Int {
    return when {
        obj is Object -> obj.hashCode()
        obj is Annotation && Proxy.isProxyClass(obj::class.java) -> obj.hashCode()
        obj != null -> System.identityHashCode(obj)
        else -> // Throw the same exception that the JVM would throw in this case.
            throw NullPointerException().sanitise(1)
    }
}

/**
 * Method references to forbidden [java.lang.Object] functions
 * will be redirected here.
 */
@Suppress("unused_parameter")
fun notify(obj: Any?) {
    failApi("java.lang.Object.notify()")
}

@Suppress("unused_parameter")
fun notifyAll(obj: Any?) {
    failApi("java.lang.Object.notifyAll()")
}

@Suppress("unused_parameter")
@Throws(InterruptedException::class)
fun wait(obj: Any?) {
    failApi("java.lang.Object.wait()")
}

@Suppress("unused_parameter")
@Throws(InterruptedException::class)
fun wait(obj: Any?, timeout: kotlin.Long) {
    failApi("java.lang.Object.wait(long)")
}

@Suppress("unused_parameter", "RemoveRedundantQualifierName")
@Throws(InterruptedException::class)
fun wait(obj: Any?, timeout: kotlin.Long, nanos: kotlin.Int) {
    failApi("java.lang.Object.wait(long, int)")
}

@Throws(ClassNotFoundException::class)
internal fun Enum<*>.fromDJVMEnum(): kotlin.Enum<*> {
    return javaClass.fromDJVMType().enumConstants[ordinal()] as kotlin.Enum<*>
}

@Throws(ClassNotFoundException::class)
private fun kotlin.Enum<*>.toDJVMEnum(): Enum<*> {
    @Suppress("unchecked_cast")
    return (DJVMClass.getEnumConstants(javaClass.toDJVM()) as Array<Enum<*>>)[ordinal]
}

internal fun getEnumConstantsShared(clazz: Class<out Enum<*>>): Array<out Enum<*>>? {
    return DJVMClass.getEnumConstantsShared(clazz)
}

/**
 * Ensure that all string constants refer to the same instance of [sandbox.java.lang.String].
 * This isn't just an optimisation - the [String.intern] behaviour expects it!
 */
fun intern(s: kotlin.String): String {
    return String.toDJVM(s).intern()
}

/**
 * Determine the platform's native [sandbox.java.nio.ByteOrder] value.
 */
val nativeOrder: ByteOrder = when (java.nio.ByteOrder.nativeOrder()) {
    java.nio.ByteOrder.BIG_ENDIAN -> ByteOrder.BIG_ENDIAN
    java.nio.ByteOrder.LITTLE_ENDIAN -> ByteOrder.LITTLE_ENDIAN
    else -> throw InternalError("Unknown platform byte-order")
}

/**
 * Replacement function for [ClassLoader.getSystemClassLoader].
 * We perform no "access control" checks because we are pretending
 * that all sandbox classes exist inside this classloader.
 */
private val systemClassLoader = SandboxRuntimeContext.instance.classLoader

fun getSystemClassLoader(): ClassLoader {
    return systemClassLoader
}

/**
 * Replacement function for [ClassLoader.getSystemResourceAsStream].
 * THIS IS NOT AVAILABLE FOR USER CODE!
 */
fun getSystemResourceAsStream(name: kotlin.String): InputStream? {
    return InputStream.toDJVM(systemClassLoader.getResourceAsStream(name))
}

/**
 * Return a buffered [DataInputStream] for a system resource.
 */
fun loadSystemResource(name: kotlin.String): DataInputStream {
    val input = getSystemResourceAsStream(name) ?: throw InternalError("Missing $name")
    return DataInputStream(BufferedInputStream(input))
}

/**
 * Return [Properties] containing system's calendars.properties file.
 */
@Throws(IOException::class)
fun getCalendarProperties(): Properties {
    return Properties().apply {
        load(AccessController.doPrivileged(DJVMSystemResourceAction("calendars.properties")))
    }
}

/**
 * Replacement function for `Class<*>.forName(String, boolean, ClassLoader)` which protects
 * against users loading classes from outside the sandbox. Note that we ALWAYS use the
 * top-most instance of [SandboxClassLoader] here.
 */
@Throws(ClassNotFoundException::class)
fun classForName(className: String, initialize: kotlin.Boolean): Class<*> {
    return Class.forName(toSandbox(className), initialize, systemClassLoader)
}

/**
 * Force the qualified class name into the `sandbox.*` namespace.
 * Throw [ClassNotFoundException] anyway if we wouldn't want to
 * return the resulting sandbox class. E.g. for any of our own
 * internal classes.
 */
@Throws(ClassNotFoundException::class)
fun toSandbox(className: String): kotlin.String = toSandbox(String.fromDJVM(className))

@Throws(ClassNotFoundException::class)
private fun toSandbox(className: kotlin.String): kotlin.String {
    if (PRIMITIVE_ARRAY.matches(className)) {
        return className
    }

    val matchName = className.removePrefix(SANDBOX_PREFIX)
    val (actualName, sandboxName) = OBJECT_ARRAY.matchEntire(matchName)?.let {
        it.groupValues[2] to it.groupValues[1] + SANDBOX_PREFIX + it.groupValues[2] + ';'
    } ?: (matchName to SANDBOX_PREFIX + matchName)

    if (bannedClasses.any { it.matches(actualName) }) {
        throw ClassNotFoundException(className).sanitise(1)
    }
    return sandboxName
}

private val bannedClasses = setOf(
    "^java\\.lang\\.DJVM(.*)?\$".toRegex(),
    "^net\\.corda\\.djvm\\..*\$".toRegex(),
    "^javax?\\..*\\.DJVM(\\$.++)?\$".toRegex(),
    "^java\\.io\\.DJVM[^.]++\$".toRegex(),
    "^java\\.util\\.concurrent\\.locks\\.DJVM[^.]++\$".toRegex(),
    "^java\\.lang\\.String\\\$InitAction\$".toRegex(),
    "^[^.]++\$".toRegex()
)

/**
 * Security Providers.
 */
val securityProviders: Properties
    get() = Properties().apply {
        setDJVMProperty("security.provider.1", "sun.security.provider.Sun")
        setDJVMProperty("security.provider.2", "sun.security.rsa.SunRsaSign")
    }

private fun Properties.setDJVMProperty(key: kotlin.String, value: kotlin.String) {
    setProperty(intern(key), intern(value))
}

/**
 * Exception Management.
 *
 * This function converts a [sandbox.java.lang.Throwable] into a
 * [java.lang.Throwable] that the JVM can actually throw.
 */
fun fromDJVM(t: Throwable?): kotlin.Throwable {
    return if (t is DJVMThrowableWrapper) {
        // We must be exiting a finally block.
        t.fromDJVM()
    } else {
        try {
            val sandboxClass = t!!.javaClass

            /**
             * Someone has created a [sandbox.java.lang.Throwable]
             * and is (re?)throwing it.
             */
            if (Type.getInternalName(sandboxClass) in JVM_EXCEPTIONS) {
                // We map these exceptions to their equivalent JVM classes.
                val throwable = sandboxClass.fromDJVMType().createJavaThrowable(t)
                sandboxedExceptions[throwable] = t
                throwable
            } else {
                // Whereas the sandbox creates a synthetic throwable wrapper for these.
                val wrapperClass = loadSandboxClass(getDJVMSynthetic(sandboxClass.name))
                wrapperClass.getPrivilegedConstructor(sandboxThrowable)
                    .newInstance(t) as kotlin.Throwable
            }
        } catch (e: kotlin.Exception) {
            e.toRuleViolationError()
        }
    }
}

/**
 * Re-throw exception if it is of type [ThreadDeath] or [VirtualMachineError].
 * The [DisallowCatchingBlacklistedExceptions] emitter causes this function
 * to be invoked at the start of matching exception handlers.
 */
fun checkCatch(exception: kotlin.Throwable) {
    when (exception) {
        is ThreadDeath, is VirtualMachineError -> throw exception
    }
}

/**
 * Wraps a [java.lang.Throwable] inside a [sandbox.java.lang.Throwable].
 * This function is invoked at the beginning of a `finally` block, and
 * so does not need to return a reference to the equivalent sandboxed
 * exception. The `finally` block only needs to be able to re-throw the
 * original exception when it finishes.
 */
fun doFinally(t: kotlin.Throwable): Throwable {
    return sandboxedExceptions.remove(t) ?: DJVMThrowableWrapper(t)
}

/**
 * Converts a [java.lang.Throwable] into a [sandbox.java.lang.Throwable].
 * It is invoked at the start of each `catch` block.
 *
 * Note: [DisallowCatchingBlacklistedExceptions] means that we don't
 * need to handle [ThreadDeath] or [VirtualMachineError] here.
 */
fun doCatch(t: kotlin.Throwable): Throwable {
    if (t is SandboxRuntimeException) {
        // Don't interfere with sandbox failures!
        throw t
    }
    try {
        return t.toDJVMThrowable()
    } catch (e: kotlin.Exception) {
        throw RuleViolationError("${e::class.java.name} -> ${e.message}").sanitise(1)
    }
}

/**
 * Clean up exception stack trace for throwing.
 */
private fun <T: kotlin.Throwable> T.sanitise(firstIndex: Int): T {
    stackTrace = stackTrace.let {
        it.sliceArray(firstIndex until findEntryPointIndex(it))
    }
    return this
}

/**
 * Worker functions to convert [java.lang.Throwable] into [sandbox.java.lang.Throwable].
 */
private fun kotlin.Throwable.toDJVMThrowable(): Throwable {
    return (this as? DJVMException)?.throwable ?:
               sandboxedExceptions.remove(this) ?:
               javaClass.toDJVM<Throwable>().createDJVMThrowable(this)
}

/**
 * Creates a new [sandbox.java.lang.Throwable] from a [java.lang.Throwable],
 * which was probably thrown by the JVM itself.
 */
private fun Class<out Throwable>.createDJVMThrowable(t: kotlin.Throwable): Throwable {
    val cause = t.cause?.toDJVMThrowable()
    val message = String.toDJVM(t.message)
    return try {
        getPrivilegedConstructor(String::class.java, Throwable::class.java)
            .newInstance(message, cause) as Throwable
    } catch (_ : NoSuchMethodException) {
        when {
            /**
             * For [InvocationTargetException], [java.lang.ExceptionInInitializerError]
             * and [PrivilegedActionException] which don't allow their underlying cause
             * to be reset.
             */
            t is InvocationTargetException ->
                getPrivilegedConstructor(Throwable::class.java, String::class.java)
                    .newInstance(cause, message) as Throwable
            t is java.lang.ExceptionInInitializerError && cause != null ->
                getPrivilegedConstructor(Throwable::class.java)
                    .newInstance(cause) as Throwable
            t is PrivilegedActionException ->
                getPrivilegedConstructor(Exception::class.java)
                    .newInstance(cause as Exception) as Throwable
            else -> {
                (try {
                    getPrivilegedConstructor(String::class.java)
                        .newInstance(message)
                } catch (_: NoSuchMethodException) {
                    getPrivilegedConstructor()
                        .newInstance()
                } as Throwable).apply {
                    cause?.run(::initCause)
                }
            }
        }
    }.apply {
        stackTrace = sanitiseToDJVM(t.stackTrace)
        t.suppressed.forEach { sup ->
            addSuppressed(sup.toDJVMThrowable())
        }
    }
}

private fun Class<*>.createJavaThrowable(t: Throwable): kotlin.Throwable {
    val cause = t.cause?.fromDJVM()
    val message = String.fromDJVM(t.message)
    return try {
        getPrivilegedConstructor(kotlin.String::class.java, kotlin.Throwable::class.java)
            .newInstance(message, cause) as kotlin.Throwable
    } catch (_ : NoSuchMethodException) {
        when {
            /**
             * For [sandbox.java.lang.reflect.InvocationTargetException], [ExceptionInInitializerError]
             * and [sandbox.java.security.PrivilegedActionException] which don't allow their underlying
             * cause to be reset.
             */
            t is sandbox.java.lang.reflect.InvocationTargetException ->
                getPrivilegedConstructor(kotlin.Throwable::class.java, kotlin.String::class.java)
                    .newInstance(cause, message) as kotlin.Throwable
            t is ExceptionInInitializerError && cause != null ->
                getPrivilegedConstructor(kotlin.Throwable::class.java)
                    .newInstance(cause) as kotlin.Throwable
            t is sandbox.java.security.PrivilegedActionException ->
                getPrivilegedConstructor(kotlin.Exception::class.java)
                    .newInstance(cause as kotlin.Exception) as kotlin.Throwable
            else -> {
                (try {
                    getPrivilegedConstructor(kotlin.String::class.java)
                        .newInstance(message)
                } catch (_: NoSuchMethodException) {
                    getPrivilegedConstructor()
                        .newInstance()
                } as kotlin.Throwable).apply {
                    cause?.run(::initCause)
                }
            }
        }
    }.apply {
        stackTrace = copyFromDJVM(t.stackTrace)
        t.suppressed.forEach { sup ->
            addSuppressed(fromDJVM(sup))
        }
    }
}

private fun findEntryPointIndex(source: Array<java.lang.StackTraceElement>): Int {
    var idx = 0
    while (idx < source.size && !isEntryPoint(source[idx])) {
        ++idx
    }
    return idx
}

private fun sanitiseToDJVM(source: Array<java.lang.StackTraceElement>): Array<StackTraceElement> {
    return copyToDJVM(source, 0, findEntryPointIndex(source))
}

internal fun copyToDJVM(source: Array<java.lang.StackTraceElement>, fromIdx: Int, toIdx: Int): Array<StackTraceElement> {
    return source.sliceArray(fromIdx until toIdx).map(::toDJVM).toTypedArray()
}

private fun toDJVM(elt: java.lang.StackTraceElement) = StackTraceElement(
    String.toDJVM(elt.className),
    String.toDJVM(elt.methodName),
    String.toDJVM(elt.fileName),
    elt.lineNumber
)

private fun copyFromDJVM(source: Array<StackTraceElement>): Array<java.lang.StackTraceElement> {
    return source.map(::fromDJVM).toTypedArray()
}

private fun fromDJVM(elt: StackTraceElement): java.lang.StackTraceElement = StackTraceElement(
    String.fromDJVM(elt.className),
    String.fromDJVM(elt.methodName),
    String.fromDJVM(elt.fileName),
    elt.lineNumber
)

private val sandboxedExceptions: MutableMap<kotlin.Throwable, Throwable> = HashMap()
private val sandboxThrowable: Class<*> = Throwable::class.java

/**
 * Resource Bundles.
 */
fun getBundle(baseName: String, locale: Locale, control: ResourceBundle.Control): ResourceBundle {
    val candidateBundles = control.getCandidateLocales(baseName, locale).map { candidateLocale ->
        val resourceKey = DJVMResourceKey(baseName, candidateLocale)
        resourceCache.getOrPut(resourceKey) {
            loadResourceBundle(control, resourceKey)
        }
    }.filter {
        it != DJVMNoResource
    }

    if (candidateBundles.isEmpty()) {
        val message = "Cannot find bundle for base name $baseName, locale $locale"
        val key = "${baseName}_$locale"
        throw fromDJVM(MissingResourceException(String.toDJVM(message), String.toDJVM(key), intern("")))
    } else {
        var idx = candidateBundles.size - 1
        while (idx > 0) {
            candidateBundles[idx - 1].childOf(candidateBundles[idx])
            --idx
        }

        return candidateBundles.first()
    }
}

private fun loadResourceBundle(control: ResourceBundle.Control, key: DJVMResourceKey): ResourceBundle {
    val bundleName = control.toBundleName(key.baseName, key.locale)
    val bundle = try {
        val bundleClass = Class.forName(toSandbox(bundleName.toString()), false, systemClassLoader)
        if (ResourceBundle::class.java.isAssignableFrom(bundleClass)) {
            (bundleClass.getDeclaredConstructor().newInstance() as ResourceBundle).also {
                it.init(key.baseName, key.locale)
            }
        } else {
            DJVMNoResource
        }
    } catch (e: kotlin.Exception) {
        DJVMNoResource
    }
    resourceCache[key] = bundle
    return bundle
}

private val resourceCache = mutableMapOf<DJVMResourceKey, ResourceBundle>()

private data class DJVMResourceKey(val baseName: String, val locale: Locale)

private object DJVMNoResource : ResourceBundle() {
    override fun handleGetObject(key: String?): Any? = null
    override fun getKeys(): Enumeration<String>? = null
    override fun toDJVMString(): String = intern("NON-EXISTENT BUNDLE")
}

/**
 * Annotation handling.
 */
fun isAnnotationPresent(annotated: AnnotatedElement, annotationType: Class<out Annotation>): kotlin.Boolean {
    return annotated.isAnnotationPresent(annotationType.toRealAnnotationType())
}

fun <T: Annotation> getAnnotation(annotated: AnnotatedElement, annotationType: Class<T>): T? {
    return annotated.getAnnotation(annotationType.toRealAnnotationType())?.let { ann ->
        return annotationType.createDJVMAnnotation(ann)
    }
}

fun getAnnotations(annotated: AnnotatedElement): Array<out Annotation> {
    return annotated.annotations.toDJVMAnnotations()
}

fun <T: Annotation> getAnnotationsByType(annotated: AnnotatedElement, annotationType: Class<T>): Array<T> {
    return doPrivileged(DJVMAnnotationsByTypeAction(annotated, annotationType.toRealAnnotationType()))
        .toDJVMAnnotations(annotationType)
}

fun <T: Annotation> getDeclaredAnnotation(annotated: AnnotatedElement, annotationType: Class<T>): T? {
    return annotated.getDeclaredAnnotation(annotationType.toRealAnnotationType())?.let { ann ->
        annotationType.createDJVMAnnotation(ann)
    }
}

fun getDeclaredAnnotations(annotated: AnnotatedElement): Array<out Annotation> {
    return annotated.declaredAnnotations.toDJVMAnnotations()
}

fun <T: Annotation> getDeclaredAnnotationsByType(annotated: AnnotatedElement, annotationType: Class<T>): Array<T> {
    return doPrivileged(DJVMDeclaredAnnotationsByTypeAction(annotated, annotationType.toRealAnnotationType()))
        .toDJVMAnnotations(annotationType)
}

fun getDefaultValue(method: Method): Any? {
    @Suppress("unchecked_cast")
    val realMethod = (method.declaringClass as Class<out Annotation>)
        .toRealAnnotationType()
        .getMethod(method.name)
    return realMethod.defaultValue?.let { value ->
        DJVMAnnotationHandler.getDefaultValue(method.returnType, value)
    }
}

fun getParameterAnnotations(executable: Executable): Array<Array<out Annotation>> {
    val parameterAnnotations = executable.parameterAnnotations
    @Suppress("unchecked_cast")
    return (newInstance(Array<out Annotation>::class.java, parameterAnnotations.size) as Array<Array<out Annotation>>).also {
        for ((i, item) in parameterAnnotations.withIndex()) {
            it[i] = item.toDJVMAnnotations()
        }
    }
}

/**
 * Worker functions to convert [java.lang.annotation.Annotation]
 * into [sandbox.java.lang.annotation.Annotation].
 */
private fun <T: Annotation> Array<out kotlin.Annotation>.toDJVMAnnotations(annotationType: Class<T>): Array<T> {
    @Suppress("unchecked_cast")
    return (newInstance(annotationType, size) as Array<T>).also {
        for ((i, item) in withIndex()) {
            it[i] = annotationType.createDJVMAnnotation(item)
        }
    }
}

private fun Array<out kotlin.Annotation>.toDJVMAnnotations(): Array<out Annotation> {
    val annotations = ArrayList<Annotation>(size)
    for (item in this) {
        val annotationType = item.annotationClass.java.toSandboxAnnotationType()
        if (annotationType != null) {
            annotations.add(annotationType.createDJVMAnnotation(item))
        }
    }
    return annotations.toArray(arrayOf<Annotation>())
}

private fun Class<out Annotation>.toRealAnnotationType(): Class<out kotlin.Annotation> {
    return if (Type.getInternalName(this) in JVM_ANNOTATIONS) {
        toDJVM()
    } else {
        @Suppress("unchecked_cast")
        loadSandboxClass(getDJVMSynthetic(name)) as Class<out kotlin.Annotation>
    }
}

private fun Class<out kotlin.Annotation>.toSandboxAnnotationType(): Class<out Annotation>? {
    val sandboxAnnotationName = systemClassLoader.resolveAnnotationName(name) ?: return null
    @Suppress("unchecked_cast")
    return loadSandboxClass(sandboxAnnotationName) as Class<out Annotation>
}

/**
 * Creates a new [sandbox.java.lang.annotation.Annotation] from a
 * [java.lang.annotation.Annotation] which was created by the JVM.
 */
fun <T: Annotation> Class<T>.createDJVMAnnotation(a: kotlin.Annotation): T {
    return AccessController.doPrivileged(DJVMAnnotationAction(this, a))
}

// This helper function MUST remain private so that it cannot
// be invoked by untrusted code!
private fun <T> doPrivileged(action: PrivilegedExceptionAction<T>): T {
    return try {
        AccessController.doPrivileged(action)
    } catch (e: PrivilegedActionException) {
        throw e.cause ?: e
    }
}

private class DJVMConstructorAction<T>(
    private val clazz: Class<T>,
    private val argTypes: Array<out Class<*>>
) : PrivilegedExceptionAction<Constructor<T>> {
    private fun isAccessible(modifiers: Int): kotlin.Boolean {
        return isPublic(modifiers) || (!isPrivate(modifiers) && clazz.`package`.name == "sandbox.java.lang")
    }

    @Throws(kotlin.Exception::class)
    override fun run(): Constructor<T> {
        return clazz.getDeclaredConstructor(*argTypes).apply {
            if (!isAccessible(modifiers)) {
                isAccessible = true
            }
        }
    }
}

private fun <T> Class<T>.getPrivilegedConstructor(vararg args: Class<*>): Constructor<T> {
    return doPrivileged(DJVMConstructorAction(this, args))
}

private class DJVMEnumAction(
    private val clazz: Class<out Enum<*>>
) : PrivilegedExceptionAction<Array<out Enum<*>>?> {
    @Throws(kotlin.Exception::class)
    override fun run(): Array<out Enum<*>>? {
        @Suppress("unchecked_cast")
        return clazz.getMethod("values").run {
            isAccessible = true
            invoke(null) as? Array<out Enum<*>>
        }
    }
}

private class DJVMSystemResourceAction(private val name: kotlin.String) : PrivilegedAction<DataInputStream?> {
    override fun run(): DataInputStream {
        return loadSystemResource(name)
    }
}

private class DJVMBootstrapClassAction(private val name: kotlin.String) : PrivilegedExceptionAction<Class<*>> {
    @Throws(ClassNotFoundException::class)
    override fun run(): Class<*> {
        return Class.forName(name, false, null)
    }
}

private class DJVMAnnotationAction<T: Annotation>(
    private val annotationType: Class<T>,
    private val underlying: kotlin.Annotation
) : PrivilegedAction<T> {
    override fun run(): T {
        @Suppress("unchecked_cast")
        return Proxy.newProxyInstance(
            systemClassLoader,
            arrayOf(annotationType),
            DJVMAnnotationHandler(annotationType, underlying)
        ) as T
    }
}

private class DJVMAnnotationsByTypeAction(
    private val annotated: AnnotatedElement,
    private val annotationType: Class<out kotlin.Annotation>
) : PrivilegedExceptionAction<Array<out kotlin.Annotation>> {
    override fun run(): Array<out kotlin.Annotation> {
        return annotated.getAnnotationsByType(annotationType)
    }
}

private class DJVMDeclaredAnnotationsByTypeAction(
    private val annotated: AnnotatedElement,
    private val annotationType: Class<out kotlin.Annotation>
) : PrivilegedExceptionAction<Array<out kotlin.Annotation>> {
    override fun run(): Array<out kotlin.Annotation> {
        return annotated.getDeclaredAnnotationsByType(annotationType)
    }
}
