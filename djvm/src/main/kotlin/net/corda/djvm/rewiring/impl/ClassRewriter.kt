package net.corda.djvm.rewiring.impl

import net.corda.djvm.SandboxConfiguration
import net.corda.djvm.analysis.AnalysisContext
import net.corda.djvm.code.impl.ClassMutator
import net.corda.djvm.code.impl.DJVM_SYNTHETIC
import net.corda.djvm.code.impl.SandboxClassWriter
import net.corda.djvm.code.impl.SandboxRemapper
import net.corda.djvm.code.impl.SyntheticAnnotationFactory
import net.corda.djvm.code.impl.SyntheticRemapper
import net.corda.djvm.rewiring.ByteCode
import net.corda.djvm.source.SourceClassLoader
import net.corda.djvm.utilities.loggerFor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassReader.SKIP_FRAMES
import org.objectweb.asm.ClassWriter.COMPUTE_FRAMES
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
        val mutator = ClassMutator(
            classVisitor = writer,
            configuration = analysisConfig,
            remapper = remapper,
            definitionProviders = configuration.definitionProviders,
            emitters = configuration.emitters
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
    }
}
