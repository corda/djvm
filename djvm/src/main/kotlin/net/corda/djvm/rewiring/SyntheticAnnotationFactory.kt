package net.corda.djvm.rewiring

import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.ClassAndMemberVisitor.Companion.API_VERSION
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import java.util.function.Consumer
import java.util.function.Function

class SyntheticAnnotationFactory(
    cv: ClassVisitor,
    private val remapper: SyntheticRemapper,
    private val configuration: AnalysisConfiguration
) : ClassVisitor(API_VERSION, cv) {

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<String>?
    ) {
        super.visit(version, access, remapper.mapAnnotationName(name), signature, superName, interfaces)
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        val mappedDescriptor = remapper.mapAnnotationDesc(descriptor)
        val av = super.visitAnnotation(mappedDescriptor, visible)
        return if (mappedDescriptor == descriptor) {
            av
        } else {
            val transformer = AnnotationTransformer(api, av, configuration)

            /*
             * Check whether we want to preserve the original annotation.
             * This will be "stitched back" under its transformed version.
             */
            if (visible && descriptor in configuration.stitchedAnnotations) {
                AnnotationStitcher(api, transformer, descriptor, Consumer { ann ->
                    ann.accept(cv, Function.identity())
                })
            } else {
                transformer
            }
        }
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        val methodDescriptor = remapper.mapMethodDesc(descriptor)
        return super.visitMethod(access, name, methodDescriptor, signature, exceptions)?.let(::MethodRemapper)
    }

    private inner class MethodRemapper(mv : MethodVisitor) : MethodVisitor(api, mv) {
        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            val mappedDescriptor = remapper.mapAnnotationDesc(descriptor)
            val av = super.visitAnnotation(mappedDescriptor, visible)
            return if (mappedDescriptor == descriptor) {
                av
            } else {
                val transformer = AnnotationTransformer(api, av, configuration)

                /*
                 * Check whether we want to preserve the original annotation.
                 * This will be "stitched back" under its transformed version.
                 */
                if (visible && descriptor in configuration.stitchedAnnotations) {
                    AnnotationStitcher(api, transformer, descriptor, Consumer { ann ->
                        ann.accept(mv, Function.identity())
                    })
                } else {
                    transformer
                }
            }
        }

        override fun visitAnnotationDefault(): AnnotationVisitor? {
            return AnnotationTransformer(api, super.visitAnnotationDefault(), configuration)
        }
    }
}
