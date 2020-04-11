package net.corda.djvm.rewiring

import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.ClassAndMemberVisitor.Companion.API_VERSION
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import java.lang.annotation.ElementType

class SyntheticAnnotationFactory(
    cv: ClassVisitor,
    private val remapper: SyntheticRemapper,
    private val configuration: AnalysisConfiguration
) : ClassVisitor(API_VERSION, cv) {
    private companion object {
        private const val JAVA_LANG_ANNOTATION = "Ljava/lang/annotation/"
    }

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
        return if (descriptor.startsWith(JAVA_LANG_ANNOTATION)) {
            /*
             * Java's meta-annotations control how this annotation
             * should behave, and so we cannot replace them with
             * synthetic annotations. We might need to tweak their
             * data values though.
             */
            val av = super.visitAnnotation(descriptor, visible)
            when (descriptor.substring(JAVA_LANG_ANNOTATION.length)) {
                "Target;" ->TargetAnnotationMapper(api, av)
                "Repeatable;" -> RepeatableAnnotationMapper(api, av)
                else -> av
            }
        } else {
            val mappedDescriptor = remapper.mapAnnotationDesc(descriptor)
            val av = super.visitAnnotation(mappedDescriptor, visible)
            AnnotationTransformer(api, av, configuration)
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
        val mappedSignature = if (signature != null && methodDescriptor == "()Ljava/lang/Class;") {
            /*
             * Discard any upper or lower bounds that this generic
             * signature may have because we cannot respect either.
             */
            "()Ljava/lang/Class<*>;"
        } else {
            null
        }
        return super.visitMethod(access, name, methodDescriptor, mappedSignature, exceptions)?.let(::MethodRemapper)
    }

    private inner class MethodRemapper(mv : MethodVisitor) : MethodVisitor(api, mv) {
        override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
            val mappedDescriptor = remapper.mapAnnotationDesc(descriptor)
            val av = super.visitAnnotation(mappedDescriptor, visible)
            return AnnotationTransformer(api, av, configuration)
        }

        override fun visitAnnotationDefault(): AnnotationVisitor? {
            return AnnotationTransformer(api, super.visitAnnotationDefault(), configuration)
        }
    }

    /**
     * An annotation type effectively becomes just an interface inside the sandbox.
     * Therefore any synthetic annotation intended to target annotation types should
     * also be allowed to target interface types.
     */
    private class TargetAnnotationMapper(api: Int, av: AnnotationVisitor) : AnnotationVisitor(api, av) {
        private companion object {
            private const val ELEMENT_TYPE_DESC = "Ljava/lang/annotation/ElementType;"
            private val ANNOTATION_TYPE = ElementType.ANNOTATION_TYPE.toString()
            private val TYPE = ElementType.TYPE.toString()
        }

        override fun visitArray(name: String?): AnnotationVisitor {
            return AllowType(api, super.visitArray(name))
        }

        private class AllowType(api: Int, av: AnnotationVisitor) : AnnotationVisitor(api, av) {
            private var hasAnnotationType: Boolean = false
            private var hasType: Boolean = false

            override fun visitEnum(name: String?, descriptor: String?, value: String?) {
                super.visitEnum(name, descriptor, value)
                if (descriptor == ELEMENT_TYPE_DESC) {
                    when(value) {
                        TYPE -> hasType = true
                        ANNOTATION_TYPE -> hasAnnotationType = true
                    }
                }
            }

            override fun visitEnd() {
                if (hasAnnotationType && !hasType) {
                    av.visitEnum(null, ELEMENT_TYPE_DESC, TYPE)
                }
                super.visitEnd()
            }
        }
    }

    /**
     * Redirect our [java.lang.annotation.Repeatable] synthetic annotation
     * to its synthetic container annotation.
     */
    private inner class RepeatableAnnotationMapper(api: Int, av: AnnotationVisitor) : AnnotationVisitor(api, av) {
        override fun visit(name: String?, value: Any?) {
            val mappedValue = if (value is Type) {
                remapper.mapAnnotation(value)
            } else {
                value
            }
            super.visit(name, mappedValue)
        }
    }
}
