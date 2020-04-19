package net.corda.djvm.rewiring

import net.corda.djvm.analysis.ClassResolver
import net.corda.djvm.analysis.Whitelist
import net.corda.djvm.code.CLASS_NAME
import net.corda.djvm.code.DJVM_NAME
import net.corda.djvm.code.OBJECT_NAME
import org.objectweb.asm.*
import org.objectweb.asm.commons.Remapper
import java.util.Collections.unmodifiableSet

/**
 * Class name and descriptor re-mapper for use in a sandbox.
 *
 * @property classResolver Functionality for resolving the class name of a sandboxed or sandboxable class.
 * @property whitelist Identifies the Java APIs which are not mapped into the sandbox namespace.
 */
open class SandboxRemapper(
        private val classResolver: ClassResolver,
        private val whitelist: Whitelist
) : Remapper() {
    private val mapped = unmodifiableSet(setOf(CLASS_NAME, OBJECT_NAME))

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
        return rewriteTypeName(super.map(typename))
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
        return when(handle.tag) {
            Opcodes.H_INVOKEVIRTUAL ->
                if (handle.owner in mapped && handle.desc.startsWith("()")) {
                    Handle(
                        Opcodes.H_INVOKESTATIC,
                        DJVM_NAME,
                        handle.name,
                        "(L${handle.owner};)" + rewriteDescriptor(handle.desc.drop("()".length)),
                        false
                    )
                } else {
                    handle
                }
            else -> handle
        }
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
    protected open fun rewriteDescriptor(descriptor: String) =
            classResolver.resolveDescriptor(descriptor)

    /**
     * Function for rewriting a type name.
     */
    protected open fun rewriteTypeName(name: String) =
            classResolver.resolve(name)

}
