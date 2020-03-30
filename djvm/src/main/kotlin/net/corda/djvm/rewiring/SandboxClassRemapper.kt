package net.corda.djvm.rewiring

import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.AnalysisConfiguration.Companion.KOTLIN_METADATA
import net.corda.djvm.analysis.ClassAndMemberVisitor.Companion.API_VERSION
import net.corda.djvm.references.MemberInformation
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ACC_ANNOTATION
import org.objectweb.asm.commons.ClassRemapper
import java.util.function.Consumer
import java.util.function.Function

class SandboxClassRemapper(
    classNonMapper: ClassVisitor,
    remapper: SandboxRemapper,
    private val configuration: AnalysisConfiguration
) : ClassRemapper(classNonMapper, remapper) {
    companion object {
        @JvmField
        val RETURNS_STRING = "\\)\\[*Ljava/lang/String;\$".toRegex()
    }

    private var classAccess: Int = 0

    private val isAnnotationClass: Boolean get() = (classAccess and ACC_ANNOTATION) != 0

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<String>?
    ) {
        classAccess = access
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        return if (configuration.isUnmappedAnnotation(descriptor)) {
            cv.visitAnnotation(descriptor, visible)
        } else if (configuration.isMappedAnnotation(descriptor)) {
            super.visitAnnotation(descriptor, visible)?.let {
                val av = if (visible && descriptor in configuration.stitchedAnnotations) {
                    AnnotationStitcher(api, it, descriptor, Consumer { ann ->
                        ann.accept(cv, Function.identity())
                    })
                } else {
                    it
                }

                /**
                 * Remap all of the descriptors within Kotlin's [Metadata] annotation.
                 * THIS ASSUMES THAT WE WILL NEVER WHITELIST KOTLIN CLASSES!!
                 */
                if (descriptor == KOTLIN_METADATA) {
                    KotlinMetadataVisitor(api, av, remapper)
                } else {
                    av
                }
            }
        } else {
            /**
             * This annotation is neither mapped nor unmapped, i.e. we drop it.
             * We cannot accept arbitrary annotations inside the sandbox until
             * we can handle annotations with [Enum] methods safely.
             */
            null
        }
    }

    /**
     * Annotation methods can return [String], [Enum] or primitive types. Obviously
     * primitives types are no problem as they are the same both inside and outside
     * the sandbox. Users can define their own [Enum] classes and so these MUST be
     * sandboxed. However, the JVM cannot handle methods with return types that use
     * [sandbox.java.lang.String] and so don't map these.
     *
     * The assumption here is that code running inside the sandbox will not need to
     * inspect its own annotations.
     */
    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        return if (isAnnotationClass && RETURNS_STRING.containsMatchIn(descriptor)) {
            cv.visitMethod(access, name, descriptor, signature, exceptions)
        } else {
            super.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }

    override fun createMethodRemapper(mv: MethodVisitor): MethodVisitor {
        return MethodRemapperWithTemplating(mv, super.createMethodRemapper(mv))
    }

    /**
     * Do not attempt to remap references to methods and fields on template classes.
     * For example, [sandbox.recordAllocation] and [sandbox.recordArrayAllocation]
     * really DO use [java.lang.String] rather than [sandbox.java.lang.String].
     */
    private inner class MethodRemapperWithTemplating(private val methodNonMapper: MethodVisitor, remapper: MethodVisitor)
        : MethodVisitor(API_VERSION, remapper) {

        private fun mapperFor(element: Element): MethodVisitor {
            return if (configuration.isTemplateClass(element.className) || isUnmapped(element)) {
                methodNonMapper
            } else {
                mv
            }
        }

        /**
         * Methods may have annotations that could need stitching, so ensure we visit these too.
         */
        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            return super.visitAnnotation(descriptor, visible)?.let {
                if (visible && descriptor in configuration.stitchedAnnotations) {
                    AnnotationStitcher(api, it, descriptor, Consumer { ann ->
                        ann.accept(methodNonMapper, Function.identity())
                    })
                } else {
                    it
                }
            }
        }

        override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
            val method = Element(owner, name, descriptor)
            return mapperFor(method).visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }

        override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
            val field = Element(owner, name, descriptor)
            return mapperFor(field).visitFieldInsn(opcode, owner, name, descriptor)
        }
    }

    private fun isUnmapped(element: Element): Boolean = configuration.whitelist.matches(element.reference)

    private data class Element(
        override val className: String,
        override val memberName: String,
        override val descriptor: String
    ) : MemberInformation
}