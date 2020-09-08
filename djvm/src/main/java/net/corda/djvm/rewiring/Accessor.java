package net.corda.djvm.rewiring;

import net.corda.djvm.analysis.AnalysisContext;
import net.corda.djvm.analysis.impl.ClassAndMemberVisitor;
import net.corda.djvm.rewiring.impl.ClassRewriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;

import java.security.CodeSource;

import static org.objectweb.asm.ClassReader.SKIP_FRAMES;

/**
 * This class only exists to prevent Kotlin from exposing OSGi private
 * types {@link ClassAndMemberVisitor} and {@link ClassRewriter} via
 * public synthetic functions and constructors!
 * Happily and deliberately package private!
 */
final class Accessor {
    private final ClassAndMemberVisitor analyzer;
    private final ClassRewriter rewriter;

    Accessor(@NotNull ClassAndMemberVisitor analyzer, @NotNull ClassRewriter rewriter) {
        this.analyzer = analyzer;
        this.rewriter = rewriter;
    }

    @NotNull
    ByteCode rewrite(@NotNull ClassReader reader, @NotNull CodeSource codeSource, @NotNull AnalysisContext context) {
        return rewriter.rewrite(reader, codeSource, context);
    }

    @NotNull
    ByteCode generateAnnotation(@NotNull ClassReader reader, @Nullable CodeSource codeSource) {
        return rewriter.generateAnnotation(reader, codeSource);
    }

    void analyze(@NotNull ClassReader reader, @NotNull AnalysisContext context) {
        analyzer.analyze(reader, context, SKIP_FRAMES);
    }
}
