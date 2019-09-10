package net.corda.djvm.rewiring

import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.ClassAndMemberVisitor.Companion.API_VERSION
import net.corda.djvm.references.MemberInformation
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ACC_ANNOTATION
import org.objectweb.asm.commons.ClassRemapper

class SandboxClassRemapper(
    private val nonClassMapper: ClassVisitor,
    remapper: SandboxRemapper,
    private val configuration: AnalysisConfiguration
) : ClassRemapper(nonClassMapper, remapper) {
    companion object {
        const val KOTLIN_METADATA = "Lkotlin/Metadata;"
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
        return if (AnalysisConfiguration.isUnmappedAnnotation(descriptor)) {
            nonClassMapper.visitAnnotation(descriptor, visible)
        } else {
            super.visitAnnotation(descriptor, visible)?.let {
                /**
                 * Remap all of the descriptors within Kotlin's [Metadata] annotation.
                 * THIS ASSUMES THAT WE WILL NEVER WHITELIST KOTLIN CLASSES!!
                 */
                if (descriptor == KOTLIN_METADATA) {
                    KotlinMetadataVisitor(api, it, remapper)
                } else {
                    it
                }
            }
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
            nonClassMapper.visitMethod(access, name, descriptor, signature, exceptions)
        } else {
            super.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }

    override fun createMethodRemapper(mv: MethodVisitor): MethodVisitor {
        return MethodRemapperWithPinning(mv, super.createMethodRemapper(mv))
    }

    /**
     * Do not attempt to remap references to methods and fields on pinned classes.
     * For example, the methods on [sandbox.RuntimeCostAccounter] really DO use
     * [java.lang.String] rather than [sandbox.java.lang.String].
     */
    private inner class MethodRemapperWithPinning(private val nonMethodMapper: MethodVisitor, remapper: MethodVisitor)
        : MethodVisitor(API_VERSION, remapper) {

        private fun mapperFor(element: Element): MethodVisitor {
            return if (configuration.isPinnedClass(element.className) || configuration.isTemplateClass(element.className) || isUnmapped(element)) {
                nonMethodMapper
            } else {
                mv
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