package net.corda.djvm.code.impl

import net.corda.djvm.analysis.ClassResolver
import net.corda.djvm.analysis.Whitelist
import org.objectweb.asm.*
import org.objectweb.asm.commons.Remapper
import java.util.Collections.unmodifiableSet

/**
 * Class name and descriptor re-mapper for use in a sandbox.
 *
 * @property classResolver Functionality for resolving the class name of a sandboxed or sandboxable class.
 * @property whitelist Identifies the Java APIs which are not mapped into the sandbox namespace.
 */
class SandboxRemapper(
    private val classResolver: ClassResolver,
    private val whitelist: Whitelist
) : Remapper() {
    private val objectMethodThunks = unmodifiableSet(setOf(
        "toString",
        "hashCode"
    ))

    /**
     * The underlying mapping function for descriptors.
     */
    override fun mapDesc(desc: String): String {
        return rewriteDescriptor(super.mapDesc(desc))
    }

    /**
     * The underlying mapping function for type names.
     */
    override fun map(typename: String): String {
        return rewriteTypeName(typename)
    }

    /**
     * Mapper for [Type] and [Handle] objects. The [Handle]
     * objects can be parameters to lambda bootstrap methods.
     */
    override fun mapValue(obj: Any?): Any? {
        return if (obj is Handle && whitelist.matches(obj.owner)) {
            mapWhitelistHandle(obj)
        } else {
            super.mapValue(obj)
        }
    }

    private fun mapWhitelistHandle(handle: Handle): Handle? {
        val owner = handle.owner
        return when(handle.tag) {
            Opcodes.H_INVOKEVIRTUAL ->
                when {
                    /**
                     * [java.lang.Class] is final, and so we know exactly
                     * which class the method we're intercepting is for.
                     */
                    owner == CLASS_NAME && isClassVirtualThunk(handle.name) ->
                        handle.toStaticClass(SANDBOX_CLASS_NAME)

                    /**
                     * We allow [java.lang.Object.toString] and [java.lang.Object.hashCode],
                     * but the monitor methods are forbidden! Apply appropriate thunks...
                     */
                    owner == OBJECT_NAME && (handle.name in objectMethodThunks || isMonitor(handle)) ->
                        handle.toStaticClass(DJVM_NAME)

                    /**
                     * We allow [ClassLoader.loadClass] here too, to be consistent with
                     * [net.corda.djvm.rules.implementation.DisallowNonDeterministicMethods].
                     */
                    owner == CLASSLOADER_NAME && isClassLoaderVirtualThunk(handle.name, handle.desc) ->
                        handle.toStaticClass(SANDBOX_CLASSLOADER_NAME)

                    else -> handle
                }

            Opcodes.H_INVOKESTATIC ->
                when {
                    owner == CLASSLOADER_NAME && isClassLoaderStaticThunk(handle.name) ->
                        handle.toClass(SANDBOX_CLASSLOADER_NAME)

                    owner == CLASS_NAME && handle.name == "forName" ->
                        handle.toClass(SANDBOX_CLASS_NAME)

                    else -> handle
                }

            else -> handle
        }
    }

    /**
     * Recreates a virtual method references as a
     * static method reference to a different class.
     */
    private fun Handle.toStaticClass(className: String) = Handle(
        Opcodes.H_INVOKESTATIC,
        className,
        name,
        prependArgType(owner, rewriteDescriptor(desc)),
        false
    )

    /**
     * Recreates a handle with a new class name.
     */
    private fun Handle.toClass(className: String) = Handle(
        tag, className, name, rewriteDescriptor(desc), false
    )

    private fun prependArgType(argType: String, descriptor: String): String {
        return "(L$argType;${descriptor.substring(1)}"
    }

    private fun isMonitor(handle: Handle): Boolean = isObjectMonitor(handle.name, handle.desc)

    private fun isClassLoaderVirtualThunk(name: String, descriptor: String): Boolean {
        return (name == "loadClass" && descriptor == "(Ljava/lang/String;)Ljava/lang/Class;")
            || isClassLoaderVirtualThunk(name)
    }

    /**
     * All [Object.toString] methods must be transformed to [sandbox.java.lang.Object.toDJVMString],
     * to allow the return type to change to [sandbox.java.lang.String].
     *
     * The [sandbox.java.lang.Object] class is a template and not mapped.
     */
    override fun mapMethodName(owner: String, name: String, descriptor: String): String {
        val newName = if (name == "toString" && descriptor == "()Ljava/lang/String;") {
            "toDJVMString"
        } else {
            name
        }
        return super.mapMethodName(owner, newName, descriptor)
    }

    /**
     * Function for rewriting a descriptor.
     */
    private fun rewriteDescriptor(descriptor: String) = classResolver.resolveDescriptor(descriptor)

    /**
     * Function for rewriting a type name.
     */
    private fun rewriteTypeName(name: String) = classResolver.resolve(name)

}
