package net.corda.djvm.rewiring.impl

import net.corda.djvm.SandboxConfiguration
import net.corda.djvm.analysis.AnalysisContext
import net.corda.djvm.analysis.SyntheticResolver
import net.corda.djvm.analysis.impl.ClassAndMemberVisitor.Companion.API_VERSION
import net.corda.djvm.code.impl.ClassMutator
import net.corda.djvm.code.impl.DJVM_SYNTHETIC
import net.corda.djvm.code.impl.EmitterModuleImpl
import net.corda.djvm.code.impl.SandboxClassRemapper
import net.corda.djvm.code.impl.SandboxClassWriter
import net.corda.djvm.code.impl.SandboxRemapper
import net.corda.djvm.code.impl.SyntheticAnnotationFactory
import net.corda.djvm.code.impl.SyntheticRemapper
import net.corda.djvm.code.impl.emptyAsNull
import net.corda.djvm.references.Member
import net.corda.djvm.references.MethodBody
import net.corda.djvm.rewiring.ByteCode
import net.corda.djvm.source.SourceClassLoader
import net.corda.djvm.utilities.loggerFor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_FRAMES
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ACC_ABSTRACT
import java.security.CodeSource

/**
 * Functionality for rewriting parts of a class as it is being loaded.
 *
 * @property configuration The configuration of the sandbox.
 * @property classLoader The class loader used to load the source classes that are to be rewritten.
 */
class ClassRewriter(
    private val configuration: SandboxConfiguration,
    private val classLoader: SourceClassLoader
) {
    private val analysisConfig = configuration.analysisConfiguration
    private val remapper = with(analysisConfig) { SandboxRemapper(classResolver, whitelist) }
    private val syntheticRemapper = SyntheticRemapper(analysisConfig)

    /**
     * Process class and allow user to rewrite parts/all of its content through provided hooks.
     *
     * @param reader The reader providing the byte code for the desired class.
     * @param codeSource The code-base for the source class.
     * @param context The context in which the class is being analyzed and processed.
     */
    fun rewrite(reader: ClassReader, codeSource: CodeSource, context: AnalysisContext): ByteCode {
        logger.debug("Rewriting class {}...", reader.className)
        val writer = SandboxClassWriter(reader, classLoader, analysisConfig, options = COMPUTE_FRAMES)
        val classRemapper = SandboxClassRemapper(
            ExceptionRemapper(SandboxStitcher(writer), analysisConfig.syntheticResolver),
            remapper,
            analysisConfig
        )
        val mutator = ClassMutator(
            classRemapper,
            analysisConfig,
            configuration.definitionProviders,
            configuration.emitters
        )
        mutator.analyze(reader, context, options = SKIP_FRAMES)
        return ByteCode(writer.toByteArray(), codeSource, mutator.flags)
    }

    fun generateAnnotation(reader: ClassReader, codeSource: CodeSource?): ByteCode {
        val writer = SandboxClassWriter(reader, classLoader, analysisConfig, options = COMPUTE_FRAMES)
        val annotationFactory = SyntheticAnnotationFactory(writer, syntheticRemapper, analysisConfig)
        reader.accept(annotationFactory, SKIP_FRAMES)
        return ByteCode(writer.toByteArray(), codeSource, DJVM_SYNTHETIC)
    }

    private companion object {
        private val logger = loggerFor<ClassRewriter>()
        private val GENERIC_SIGNATURE = "^<([^:]++):.*>.*".toRegex()
    }

    /**
     * Extra visitor that is applied after [SandboxRemapper]. This "stitches" the original
     * unmapped interface as a super-interface of the mapped version, as well as adding
     * or replacing any extra methods that are needed.
     */
    private inner class SandboxStitcher(parent: ClassVisitor)
        : ClassVisitor(API_VERSION, parent)
    {
        private val extraMethods = mutableListOf<Member>()

        override fun visit(version: Int, access: Int, className: String, signature: String?, superName: String?, interfaces: Array<String>?) {
            var stitchedSignature = signature
            val stitchedInterfaces = analysisConfig.stitchedInterfaces[className]?.let { methods ->
                extraMethods += methods
                val baseInterface = analysisConfig.classResolver.reverse(className)
                if (stitchedSignature != null) {
                    /*
                     * All of our stitched interfaces have a single generic
                     * parameter. This simplifies how we update the signature
                     * to include this new interface.
                     */
                    GENERIC_SIGNATURE.matchEntire(stitchedSignature)?.apply {
                        val typeVar = groupValues[1]
                        stitchedSignature += "L$baseInterface<T$typeVar;>;"
                    }
                }
                arrayOf(*(interfaces ?: emptyArray()), baseInterface)
            } ?: interfaces

            analysisConfig.stitchedClasses[className]?.also { methods ->
                extraMethods += methods
            }

            super.visit(version, access, className, stitchedSignature, superName, stitchedInterfaces)
        }

        override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            return if (extraMethods.isEmpty()) {
                super.visitMethod(access, name, descriptor, signature, exceptions)
            } else {
                val idx = extraMethods.indexOfFirst { it.memberName == name && it.descriptor == descriptor && it.genericsDetails.emptyAsNull == signature }
                if (idx != -1) {
                    val replacement = extraMethods.removeAt(idx)
                    if (replacement.body.isNotEmpty() || (access and ACC_ABSTRACT) != 0) {
                        // Replace an existing method, or delete it entirely if
                        // the replacement has no method body and isn't abstract.
                        super.visitMethod(access, name, descriptor, signature, exceptions)?.also { mv ->
                            // This COMPLETELY replaces the original method, and
                            // will also discard any annotations it may have had.
                            writeMethodBody(mv, replacement.body)
                        }
                    }
                    null
                } else {
                    super.visitMethod(access, name, descriptor, signature, exceptions)
                }
            }
        }

        override fun visitEnd() {
            for (method in extraMethods) {
                with(method) {
                    super.visitMethod(access, memberName, descriptor, genericsDetails.emptyAsNull, exceptions.toTypedArray())?.also { mv ->
                        writeMethodBody(mv, body)
                    }
                }
            }
            extraMethods.clear()
            super.visitEnd()
        }

        private fun writeMethodBody(mv: MethodVisitor, body: List<MethodBody>) {
            mv.visitCode()
            EmitterModuleImpl(mv, analysisConfig).writeByteCode(body)
            mv.visitMaxs(-1, -1)
            mv.visitEnd()
        }
    }
}

/**
 * Map exceptions in method signatures to their sandboxed equivalents.
 */
private class ExceptionRemapper(parent: ClassVisitor, private val syntheticResolver: SyntheticResolver) : ClassVisitor(API_VERSION, parent) {
    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        val mappedExceptions = exceptions?.map(syntheticResolver::getRealThrowableName)?.toTypedArray()
        return super.visitMethod(access, name, descriptor, signature, mappedExceptions)?.let(::MethodExceptionRemapper)
    }

    /**
     * Map exceptions in method try-catch blocks to their sandboxed equivalents.
     */
    private inner class MethodExceptionRemapper(parent: MethodVisitor) : MethodVisitor(api, parent) {
        override fun visitTryCatchBlock(start: Label, end: Label, handler: Label, exceptionType: String?) {
            val mappedExceptionType = exceptionType?.let(syntheticResolver::getRealThrowableName)
            super.visitTryCatchBlock(start, end, handler, mappedExceptionType)
        }
    }
}