package net.corda.djvm.analysis

import org.objectweb.asm.Type

class SyntheticResolver(
    private val jvmExceptionClasses: Set<String>,
    private val jvmAnnotationClasses: Set<String>,
    private val sandboxPrefix: String
) {
    companion object {
        private const val DJVM_SYNTHETIC_NAME = "\$1DJVM"

        fun isDJVMSynthetic(className: String): Boolean = className.endsWith(DJVM_SYNTHETIC_NAME)
        fun getDJVMSynthetic(className: String): String = className + DJVM_SYNTHETIC_NAME
        fun getDJVMSyntheticOwner(className: String): String = className.dropLast(DJVM_SYNTHETIC_NAME.length)

        @JvmStatic
        fun getDJVMSyntheticDescriptor(descriptor: String): String {
            return descriptor.dropLast(1) + DJVM_SYNTHETIC_NAME + ';'
        }
    }

    fun getThrowableName(clazz: Class<*>): String {
        return getDJVMSynthetic(Type.getInternalName(clazz))
    }

    fun getThrowableSuperName(clazz: Class<*>): String {
        return getRealThrowableName(Type.getInternalName(clazz.superclass))
    }

    fun getRealThrowableName(internalName: String): String {
        return if (internalName in jvmExceptionClasses) {
            internalName.unsandboxed
        } else {
            getDJVMSynthetic(internalName)
        }
    }

    fun getAnnotationName(clazz: Class<*>): String {
        return getRealAnnotationName(Type.getInternalName(clazz))
    }

    fun getRealAnnotationName(internalName: String): String {
        return if (internalName in jvmAnnotationClasses) {
            internalName.unsandboxed
        } else {
            getDJVMSynthetic(internalName)
        }
    }

    private val String.unsandboxed: String get() = if (startsWith(sandboxPrefix)) {
        drop(sandboxPrefix.length)
    } else {
        this
    }
}