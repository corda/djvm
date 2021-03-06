package net.corda.djvm.code.impl

import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.AnalysisConfiguration.Companion.KOTLIN_METADATA
import net.corda.djvm.analysis.SyntheticResolver.Companion.getDJVMSyntheticDescriptor
import net.corda.djvm.analysis.impl.ClassAndMemberVisitor.Companion.API_VERSION
import net.corda.djvm.references.MemberInformation
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.TypePath
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import java.util.function.Consumer
import java.util.function.Function

class SandboxClassRemapper(
    classNonMapper: ClassVisitor,
    remapper: Remapper,
    private val configuration: AnalysisConfiguration
) : ClassRemapper(API_VERSION, classNonMapper, remapper) {

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        return if (configuration.isJvmAnnotationDesc(descriptor)) {
            /*
             * The annotation can be preserved "as is" because
             * it has no data fields that need transforming.
             */
            cv.visitAnnotation(descriptor, visible)
        } else {
            super.visitAnnotation(getDJVMSyntheticDescriptor(descriptor), visible)?.let {
                val transformer = AnnotationTransformer(api, it, configuration)

                /*
                 * Check whether we want to preserve the original annotation.
                 * This will be "stitched back" under its transformed version.
                 */
                val av = if (visible && descriptor in configuration.stitchedAnnotations) {
                    AnnotationStitcher(api, transformer, descriptor, Consumer { ann ->
                        ann.accept(cv, Function.identity())
                    })
                } else {
                    transformer
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
        }
    }

    /**
     * Drop these annotations because we aren't handling them - yet?
     */
    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean): AnnotationVisitor? = null

    override fun createMethodRemapper(mv: MethodVisitor): MethodVisitor {
        return MethodRemapperWithTemplating(super.createMethodRemapper(mv), mv)
    }

    override fun createFieldRemapper(fv: FieldVisitor): FieldVisitor {
        return FieldRemapper(super.createFieldRemapper(fv), fv)
    }

    /**
     * Do not attempt to remap references to methods and fields on template classes.
     * For example, [sandbox.recordAllocation] and [sandbox.recordArrayAllocation]
     * really DO use [java.lang.String] rather than [sandbox.java.lang.String].
     */
    private inner class MethodRemapperWithTemplating(remapper: MethodVisitor, private val methodNonMapper: MethodVisitor)
        : MethodVisitor(api, remapper) {

        private fun mapperFor(element: Element): MethodVisitor {
            return if (configuration.classResolver.isTemplateClass(element.className) || isUnmapped(element)) {
                methodNonMapper
            } else {
                mv
            }
        }

        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            return super.visitAnnotation(getDJVMSyntheticDescriptor(descriptor), visible)?.let { av ->
                val transformer = AnnotationTransformer(api, av, configuration)

                /*
                 * Check whether we want to preserve the original annotation.
                 * This will be "stitched back" under its transformed version.
                 */
                if (visible && descriptor in configuration.stitchedAnnotations) {
                    AnnotationStitcher(api, transformer, descriptor, Consumer { ann ->
                        ann.accept(methodNonMapper, Function.identity())
                    })
                } else {
                    transformer
                }
            }
        }

        override fun visitParameterAnnotation(parameter: Int, descriptor: String, visible: Boolean): AnnotationVisitor? {
            return super.visitParameterAnnotation(parameter, getDJVMSyntheticDescriptor(descriptor), visible)?.let { av ->
                val transformer = AnnotationTransformer(api, av, configuration)

                /*
                 * Check whether we want to preserve the original annotation.
                 * This will be "stitched back" under its transformed version.
                 */
                if (visible && descriptor in configuration.stitchedAnnotations) {
                    AnnotationStitcher(api, transformer, descriptor, Consumer { ann ->
                        ann.accept(methodNonMapper, parameter, Function.identity())
                    })
                } else {
                    transformer
                }
            }
        }

        /**
         * An annotation class is just an interface after it has been mapped,
         * so remove special "default value" attributes from its methods.
         */
        override fun visitAnnotationDefault(): AnnotationVisitor? = null

        override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
            val method = Element(owner, name, descriptor)
            return mapperFor(method).visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        }

        override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
            val field = Element(owner, name, descriptor)
            return mapperFor(field).visitFieldInsn(opcode, owner, name, descriptor)
        }

        /**
         * Drop these annotations because we aren't handling them - yet?
         */
        override fun visitInsnAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean): AnnotationVisitor? = null
        override fun visitLocalVariableAnnotation(
            typeRef: Int,
            typePath: TypePath?,
            start: Array<out Label>?,
            end: Array<out Label>?,
            index: IntArray?,
            descriptor: String,
            visible: Boolean
        ): AnnotationVisitor? = null
        override fun visitTryCatchAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean): AnnotationVisitor? = null
        override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean): AnnotationVisitor? = null
    }

    private inner class FieldRemapper(remapper: FieldVisitor, private val fieldNonMapper: FieldVisitor) : FieldVisitor(api, remapper) {
        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            return super.visitAnnotation(getDJVMSyntheticDescriptor(descriptor), visible)?.let { av ->
                val transformer = AnnotationTransformer(api, av, configuration)

                /*
                 * Check whether we want to preserve the original annotation.
                 * This will be "stitched back" under its transformed version.
                 */
                if (visible && descriptor in configuration.stitchedAnnotations) {
                    AnnotationStitcher(api, transformer, descriptor, Consumer { ann ->
                        ann.accept(fieldNonMapper, Function.identity())
                    })
                } else {
                    transformer
                }
            }
        }

        /**
         * Drop these annotations because we aren't handling them - yet?
         */
        override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath?, descriptor: String, visible: Boolean): AnnotationVisitor? = null
    }

    private fun isUnmapped(element: Element): Boolean = configuration.whitelist.matches(element.reference)

    private data class Element(
        override val className: String,
        override val memberName: String,
        override val descriptor: String
    ) : MemberInformation
}