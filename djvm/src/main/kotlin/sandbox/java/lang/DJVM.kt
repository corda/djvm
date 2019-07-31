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
import sandbox.java.util.*
import java.lang.reflect.Constructor

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

fun kotlin.Throwable.escapeSandbox(): kotlin.Throwable {
    val sandboxed = (this as? DJVMException)?.throwable ?: sandboxedExceptions.remove(this)
    return sandboxed?.escapeSandbox() ?: sanitise(0)
}

fun Throwable.escapeSandbox(): kotlin.Throwable {
    val sandboxedName = javaClass.name
    return try {
        val escaping = if (Type.getInternalName(javaClass) in JVM_EXCEPTIONS) {
            // We map these exceptions to their equivalent JVM classes.
            @Suppress("unchecked_cast")
            val escapingType = loadBootstrapClass(sandboxedName.fromSandboxPackage()) as Class<out kotlin.Throwable>
            try {
                escapingType.getDeclaredConstructor(kotlin.String::class.java).newInstance(String.fromDJVM(message))
            } catch (e: NoSuchMethodException) {
                escapingType.newInstance()
            }
        } else {
            val escapingMessage = "$sandboxedName -> $message"
            val sourceType = loadSandboxClass(getDJVMException(sandboxedName))
            when {
                RuntimeException::class.java.isAssignableFrom(sourceType) -> RuntimeException(escapingMessage)
                Exception::class.java.isAssignableFrom(sourceType) -> Exception(escapingMessage)
                Error::class.java.isAssignableFrom(sourceType) -> Error(escapingMessage)
                else -> Throwable(escapingMessage)
            }
        }
        escaping.apply {
            this@escapeSandbox.cause?.also {
                initCause(it.escapeSandbox())
            }
            stackTrace = copyFromDJVM(this@escapeSandbox.stackTrace)
            sanitise(0)
            this@escapeSandbox.suppressed.forEach {
                addSuppressed(it.escapeSandbox())
            }
        }
    } catch (e: Exception) {
        RuleViolationError(e.message).sanitise(1)
    }
}

private fun Array<*>.fromDJVMArray(): Array<*> = Object.fromDJVM(this)

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
internal fun Class<*>.toDJVMType(): Class<*> = loadSandboxClass(name.toSandboxPackage())

@Throws(ClassNotFoundException::class)
internal fun Class<*>.fromDJVMType(): Class<*> = loadSandboxClass(name.fromSandboxPackage())

private fun loadSandboxClass(name: kotlin.String): Class<*> = Class.forName(name, false, systemClassLoader)
private fun loadBootstrapClass(name: kotlin.String): Class<*> = Class.forName(name, true, null)

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

@Suppress("unchecked_cast")
internal fun getEnumConstantsShared(clazz: Class<out Enum<*>>): Array<out Enum<*>>? {
    return if (isEnum(clazz)) {
        allEnums.get(clazz) ?: createEnum(clazz)
    } else {
        null
    }
}

@Suppress("unchecked_cast")
private fun createEnum(clazz: Class<out Enum<*>>): Array<out Enum<*>>? {
    return clazz.getMethod("values").let { method ->
        method.isAccessible = true
        method.invoke(null) as? Array<out Enum<*>>
    }?.apply { allEnums.put(clazz, this) }
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
 * Replacement function for [ClassLoader.getSystemClassLoader].
 */
val systemClassLoader: ClassLoader get() {
    return SandboxRuntimeContext.instance.classLoader
}

/**
 * Filter function for [Class.getClassLoader].
 */
@Suppress("unused_parameter")
fun getClassLoader(type: Class<*>): ClassLoader {
    /**
     * We expect [Class.getClassLoader] to return one of the following:
     * - [net.corda.djvm.rewiring.SandboxClassLoader] for sandbox classes
     * - the application class loader for pinned classes
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
    // Read system resources from the classloader that contains the Java APIs.
    // I'd prefer to use this class's own classloader really, except that Kotlin
    // cannot provide me with the .class for this "file".
    return InputStream.toDJVM(sandboxThrowable.classLoader.getResourceAsStream(name))
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
    "^javax?\\..*\\.DJVM\$".toRegex(),
    "^java\\.io\\.DJVM[^.]++\$".toRegex(),
    "^java\\.util\\.concurrent\\.locks\\.DJVM[^.]++\$".toRegex(),
    "^RawTask\$".toRegex(),
    "^RuntimeCostAccounter\$".toRegex(),
    "^Task(.*)?\$".toRegex()
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
                loadSandboxClass(getDJVMException(sandboxClass.name))
                    .getDeclaredConstructor(sandboxThrowable)
                    .newInstance(t) as kotlin.Throwable
            }
        } catch (e: Exception) {
            RuleViolationError(e.message).sanitise(1)
        }
    }
}

/**
 * Wraps a [java.lang.Throwable] inside a [sandbox.java.lang.Throwable].
 * This function is invoked at the beginning of a finally block, and
 * so does not need to return a reference to the equivalent sandboxed
 * exception. The finally block only needs to be able to re-throw the
 * original exception when it finishes.
 */
fun finally(t: kotlin.Throwable): Throwable {
    return sandboxedExceptions.remove(t) ?: DJVMThrowableWrapper(t)
}

/**
 * Converts a [java.lang.Throwable] into a [sandbox.java.lang.Throwable].
 * It is invoked at the start of each catch block.
 *
 * Note: [DisallowCatchingBlacklistedExceptions] means that we don't
 * need to handle [ThreadDeath] or [VirtualMachineError] here.
 */
fun catch(t: kotlin.Throwable): Throwable {
    if (t is SandboxClassLoadingException) {
        // Don't interfere with sandbox failures!
        throw t
    }
    try {
        return t.toDJVMThrowable()
    } catch (e: Exception) {
        throw RuleViolationError(e.message).sanitise(1)
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
    return (try {
        getDeclaredConstructor(String::class.java).newInstance(String.toDJVM(t.message))
    } catch (e: NoSuchMethodException) {
        newInstance()
    } as Throwable).apply {
        t.cause?.also {
            initCause(it.toDJVMThrowable())
        }
        stackTrace = sanitiseToDJVM(t.stackTrace)
        t.suppressed.forEach {
            addSuppressed(it.toDJVMThrowable())
        }
    }
}

private fun Class<*>.createJavaThrowable(t: Throwable): kotlin.Throwable {
    return (try {
        getDeclaredConstructor(kotlin.String::class.java).newInstance(String.fromDJVM(t.message))
    } catch (e: NoSuchMethodException) {
        newInstance()
    } as kotlin.Throwable).apply {
        t.cause?.also {
            initCause(fromDJVM(it))
        }
        stackTrace = copyFromDJVM(t.stackTrace)
        t.suppressed.forEach {
            addSuppressed(fromDJVM(it))
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
    control.getCandidateLocales(baseName, locale).forEach { candidateLocale ->
        val resourceKey = DJVMResourceKey(baseName, candidateLocale)
        val bundle = resourceCache[resourceKey] ?: run {
            loadResourceBundle(control, resourceKey)
        }

        if (bundle != DJVMNoResource) {
            return bundle
        }
    }

    val message = "Cannot find bundle for base name $baseName, locale $locale"
    val key = "${baseName}_$locale"
    throw fromDJVM(MissingResourceException(String.toDJVM(message), String.toDJVM(key), intern("")))
}

private fun loadResourceBundle(control: ResourceBundle.Control, key: DJVMResourceKey): ResourceBundle {
    val bundleName = control.toBundleName(key.baseName, key.locale)
    val bundle = try {
        val bundleClass = systemClassLoader.loadClass(toSandbox(bundleName.toString()))
        if (ResourceBundle::class.java.isAssignableFrom(bundleClass)) {
            bundleClass.newInstance() as ResourceBundle
        } else {
            DJVMNoResource
        }
    } catch (e: Exception) {
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
