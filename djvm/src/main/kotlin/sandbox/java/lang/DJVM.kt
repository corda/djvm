@file:JvmName("DJVM")
@file:Suppress("unused")
package sandbox.java.lang

import net.corda.djvm.SandboxRuntimeContext
import net.corda.djvm.analysis.AnalysisConfiguration.Companion.JVM_EXCEPTIONS
import net.corda.djvm.analysis.ExceptionResolver.Companion.getDJVMException
import net.corda.djvm.rewiring.SandboxClassLoadingException
import net.corda.djvm.rules.RuleViolationError
import net.corda.djvm.rules.implementation.*
import org.objectweb.asm.Opcodes.ACC_ENUM
import org.objectweb.asm.Type
import sandbox.isEntryPoint
import sandbox.java.io.*
import sandbox.java.nio.ByteOrder
import sandbox.java.util.*
import java.io.IOException
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier.*
import java.security.AccessController
import java.security.PrivilegedAction
import java.security.PrivilegedActionException
import java.security.PrivilegedExceptionAction

private const val SANDBOX_PREFIX = "sandbox."
private val OBJECT_ARRAY = "^(\\[++L)([^;]++);\$".toRegex()
private val PRIMITIVE_ARRAY = "^(\\[)++[IJSCBZFD]\$".toRegex()

fun Any.unsandbox(): Any {
    return when (this) {
        is Object -> fromDJVM()
        is Array<*> -> fromDJVMArray()
        else -> this
    }
}

@Throws(ClassNotFoundException::class)
fun Any.sandbox(): Any {
    return when (this) {
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

        // These types are white-listed inside the sandbox, which
        // means that they're used "as is". So prevent the user
        // from passing bad instances into the sandbox through the
        // front door!
        is Class<*>, is Constructor<*> -> throw RuleViolationError("Cannot sandbox $this").sanitise(1)
        is ClassLoader -> throw RuleViolationError("Cannot sandbox a ClassLoader").sanitise(1)

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
            val sourceType = loadSandboxClass(getDJVMException(sandboxedName))
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

/**
 * Use [Class.forName] so that we can also fetch classes for arrays of primitive types.
 * Also use the sandbox's classloader explicitly here, because this invoking class
 * might belong to a shared parent classloader.
 */
@Throws(ClassNotFoundException::class)
private fun Class<*>.toDJVMType(): Class<*> = loadSandboxClass(name.toSandboxPackage())

@Throws(ClassNotFoundException::class)
internal fun Class<*>.fromDJVMType(): Class<*> = loadSandboxClass(name.fromSandboxPackage())

private fun loadSandboxClass(name: kotlin.String): Class<*> = Class.forName(name, false, systemClassLoader)
private fun loadBootstrapClass(name: kotlin.String): Class<*> = doPrivileged(DJVMBootstrapClassAction(name))

private fun kotlin.String.toSandboxPackage(): kotlin.String {
    return if (startsWith(SANDBOX_PREFIX)) {
        this
    } else {
        SANDBOX_PREFIX + this
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
    return (java.lang.reflect.Array.newInstance(javaClass.componentType.toDJVMComponentType(), size) as Array<Any?>).also {
        for ((i, item) in withIndex()) {
            it[i] = item?.sandbox()
        }
    }
}

private fun Class<*>.toDJVMComponentType(): Class<*> {
    return if (isArray || isAssignableFrom(java.io.Serializable::class.java) || isAssignableFrom(Cloneable::class.java)) {
        // Serializable, Cloneable and array types don't have sandbox.* equivalents.
        this
    } else {
        toDJVMType()
    }
}

@Throws(ClassNotFoundException::class)
internal fun Enum<*>.fromDJVMEnum(): kotlin.Enum<*> {
    return javaClass.fromDJVMType().enumConstants[ordinal()] as kotlin.Enum<*>
}

@Throws(ClassNotFoundException::class)
private fun kotlin.Enum<*>.toDJVMEnum(): Enum<*> {
    @Suppress("unchecked_cast")
    return (getEnumConstants(javaClass.toDJVMType() as Class<Enum<*>>) as Array<Enum<*>>)[ordinal]
}

/**
 * Replacement functions for the members of Class<*> that support Enums.
 */
fun isEnum(clazz: Class<*>): kotlin.Boolean
        = (clazz.modifiers and ACC_ENUM != 0) && (clazz.superclass == sandbox.java.lang.Enum::class.java)

fun getEnumConstants(clazz: Class<out Enum<*>>): Array<*>? {
    return getEnumConstantsShared(clazz)?.clone()
}

internal fun enumConstantDirectory(clazz: Class<out Enum<*>>): sandbox.java.util.Map<String, out Enum<*>>? {
    return allEnumDirectories.get(clazz) ?: createEnumDirectory(clazz)
}

internal fun getEnumConstantsShared(clazz: Class<out Enum<*>>): Array<out Enum<*>>? {
    return if (isEnum(clazz)) {
        allEnums.get(clazz) ?: createEnum(clazz)
    } else {
        null
    }
}

private fun createEnum(clazz: Class<out Enum<*>>): Array<out Enum<*>>? {
    return doPrivileged(DJVMEnumAction(clazz))?.apply { allEnums.put(clazz, this) }
}

private fun createEnumDirectory(clazz: Class<out Enum<*>>): sandbox.java.util.Map<String, out Enum<*>> {
    val universe = getEnumConstantsShared(clazz) ?: throw IllegalArgumentException("${clazz.name} is not an enum type")
    val directory = LinkedHashMap<String, Enum<*>>(2 * universe.size)
    for (entry in universe) {
        directory.put(entry.name(), entry)
    }
    allEnumDirectories.put(clazz, directory)
    return directory
}

private val allEnums: sandbox.java.util.Map<Class<out Enum<*>>, Array<out Enum<*>>> = LinkedHashMap()
private val allEnumDirectories: sandbox.java.util.Map<Class<out Enum<*>>, sandbox.java.util.Map<String, out Enum<*>>> = LinkedHashMap()

/**
 * Replacement function for Object.hashCode(), because some objects
 * (i.e. arrays) cannot be replaced by [sandbox.java.lang.Object].
 */
fun hashCode(obj: Any?): Int {
    return when {
        obj is Object -> obj.hashCode()
        obj != null -> System.identityHashCode(obj)
        else -> // Throw the same exception that the JVM would throw in this case.
            throw NullPointerException().sanitise(1)
    }
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
 * Filter function for [Class.getClassLoader].
 * We perform no "access control" checks because we are pretending
 * that all sandbox classes exist inside the same classloader.
 */
@Suppress("unused_parameter")
fun getClassLoader(type: Class<*>): ClassLoader {
    /**
     * We expect [Class.getClassLoader] to return one of the following:
     * - [net.corda.djvm.rewiring.SandboxClassLoader] for sandbox classes
     * - the application class loader for whitelisted classes
     * - null for basic Java classes.
     *
     * So "don't do that". Always return the sandbox classloader instead.
     */
    return systemClassLoader
}

/**
 * Replacement function for [ClassLoader.getSystemResourceAsStream].
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
 * Replacement function for Class<*>.forName(String, boolean, ClassLoader) which protects
 * against users loading classes from outside the sandbox.
 */
@Throws(ClassNotFoundException::class)
fun classForName(className: kotlin.String, initialize: kotlin.Boolean, classLoader: ClassLoader?): Class<*> {
    return Class.forName(toSandbox(className), initialize, classLoader ?: systemClassLoader)
}

@Throws(ClassNotFoundException::class)
fun loadClass(classLoader: ClassLoader, className: kotlin.String): Class<*> {
    return classLoader.loadClass(toSandbox(className))
}

/**
 * Force the qualified class name into the sandbox.* namespace.
 * Throw [ClassNotFoundException] anyway if we wouldn't want to
 * return the resulting sandbox class. E.g. for any of our own
 * internal classes.
 */
@Throws(ClassNotFoundException::class)
fun toSandbox(className: kotlin.String): kotlin.String {
    if (PRIMITIVE_ARRAY.matches(className)) {
        return className
    }

    val matchName = className.removePrefix(SANDBOX_PREFIX)
    val (actualName, sandboxName) = OBJECT_ARRAY.matchEntire(matchName)?.let {
        Pair(it.groupValues[2], it.groupValues[1] + SANDBOX_PREFIX + it.groupValues[2] + ';')
    } ?: Pair(matchName, SANDBOX_PREFIX + matchName)

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
    setProperty(String.toDJVM(key), String.toDJVM(value))
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
                val wrapperClass = loadSandboxClass(getDJVMException(sandboxClass.name))
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
 * This function is invoked at the beginning of a finally block, and
 * so does not need to return a reference to the equivalent sandboxed
 * exception. The finally block only needs to be able to re-throw the
 * original exception when it finishes.
 */
fun doFinally(t: kotlin.Throwable): Throwable {
    return sandboxedExceptions.remove(t) ?: DJVMThrowableWrapper(t)
}

/**
 * Converts a [java.lang.Throwable] into a [sandbox.java.lang.Throwable].
 * It is invoked at the start of each catch block.
 *
 * Note: [DisallowCatchingBlacklistedExceptions] means that we don't
 * need to handle [ThreadDeath] or [VirtualMachineError] here.
 */
fun doCatch(t: kotlin.Throwable): Throwable {
    if (t is SandboxClassLoadingException) {
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
               javaClass.toDJVMType().createDJVMThrowable(this)
}

/**
 * Creates a new [sandbox.java.lang.Throwable] from a [java.lang.Throwable],
 * which was probably thrown by the JVM itself.
 */
private fun Class<*>.createDJVMThrowable(t: kotlin.Throwable): Throwable {
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
        val bundleClass = systemClassLoader.loadClass(toSandbox(bundleName.toString()))
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
    override fun run(): DataInputStream? {
        return loadSystemResource(name)
    }
}

private class DJVMBootstrapClassAction(private val name: kotlin.String) : PrivilegedExceptionAction<Class<*>> {
    @Throws(ClassNotFoundException::class)
    override fun run(): Class<*> {
        return Class.forName(name, false, null)
    }
}
