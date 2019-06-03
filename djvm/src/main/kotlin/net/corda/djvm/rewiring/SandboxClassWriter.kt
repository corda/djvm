package net.corda.djvm.rewiring

import net.corda.djvm.code.asPackagePath
import net.corda.djvm.source.SourceClassLoader
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
import org.objectweb.asm.ClassWriter.COMPUTE_MAXS
import org.objectweb.asm.Type

/**
 * Class writer for sandbox execution, with a configurable classloader to ensure correct deduction of the used class
 * hierarchy.
 *
 * @param classReader The [ClassReader] used to read the original class. It will be used to copy the entire constant
 * pool and bootstrap methods from the original class and also to copy other fragments of original byte code where
 * applicable.
 * @property cloader The class loader used to load the classes that are to be rewritten.
 * @param flags Option flags that can be used to modify the default behaviour of this class. Must be zero or a
 * combination of [COMPUTE_MAXS] and [COMPUTE_FRAMES]. These option flags do not affect methods that are copied as is
 * in the new class. This means that neither the maximum stack size nor the stack frames will be computed for these
 * methods. Note that [COMPUTE_FRAMES] implies [COMPUTE_MAXS].
 */
open class SandboxClassWriter(
        classReader: ClassReader,
        private val cloader: SourceClassLoader,
        flags: Int = COMPUTE_FRAMES
) : ClassWriter(classReader, flags) {

    override fun getClassLoader(): SourceClassLoader = cloader

    /**
     * Get the common super type of [type1] and [type2].
     */
    override fun getCommonSuperClass(type1: String, type2: String): String {
        // Need to override [getCommonSuperClass] to ensure that we use SourceClassLoader.loadSourceClass().
        when {
            type1 == OBJECT_NAME -> return type1
            type2 == OBJECT_NAME -> return type2
        }
        val class1 = try {
            classLoader.loadSourceClass(type1.asPackagePath)
        } catch (exception: Exception) {
            throw TypeNotPresentException(type1, exception)
        }
        val class2 = try {
            classLoader.loadSourceClass(type2.asPackagePath)
        } catch (exception: Exception) {
            throw TypeNotPresentException(type2, exception)
        }
        return when {
            class1.isAssignableFrom(class2) -> type1
            class2.isAssignableFrom(class1) -> type2
            class1.isInterface || class2.isInterface -> OBJECT_NAME
            else -> {
                var clazz = class1
                do {
                    clazz = clazz.superclass
                } while (!clazz.isAssignableFrom(class2))
                Type.getInternalName(clazz)
            }
        }
    }

    private companion object {

        private const val OBJECT_NAME = "java/lang/Object"

    }

}
