package net.corda.djvm.analysis

import org.objectweb.asm.Type

class SyntheticResolver(
    private val jvmExceptionClasses: Set<String>,
    private val sandboxPrefix: String
) {
    companion object {
        private const val DJVM_SYNTHETIC_NAME = "\$1DJVM"

        fun isDJVMSynthetic(className: String): Boolean = className.endsWith(DJVM_SYNTHETIC_NAME)
        fun getDJVMSynthetic(className: String): String = className + DJVM_SYNTHETIC_NAME
        fun getDJVMSyntheticOwner(className: String): String = className.dropLast(DJVM_SYNTHETIC_NAME.length)
    }

    fun getThrowableName(clazz: Class<*>): String {
        return getDJVMSynthetic(Type.getInternalName(clazz))
    }

    fun getThrowableSuperName(clazz: Class<*>): String {
        return getThrowableOwnerName(Type.getInternalName(clazz.superclass))
    }

    fun getThrowableOwnerName(className: String): String {
        return if (className in jvmExceptionClasses) {
            className.unsandboxed
        } else {
            getDJVMSynthetic(className)
        }
    }

    private val String.unsandboxed: String get() = if (startsWith(sandboxPrefix)) {
        drop(sandboxPrefix.length)
    } else {
        this
    }
}