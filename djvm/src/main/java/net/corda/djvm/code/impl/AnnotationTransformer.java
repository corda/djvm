package net.corda.djvm.code.impl;

import net.corda.djvm.analysis.AnalysisConfiguration;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import static net.corda.djvm.analysis.SyntheticResolver.getDJVMSyntheticDescriptor;

/**
 * Rewrite the data attributes for a synthetic annotation.
 * Written in Java so that it can be package private.
 */
class AnnotationTransformer extends AnnotationVisitor {
    private final AnalysisConfiguration configuration;

    AnnotationTransformer(int api, @NotNull AnnotationVisitor av, AnalysisConfiguration configuration) {
        super(api, av);
        this.configuration = configuration;
    }

    @NotNull
    private String mapToSandbox(String descriptor) {
        return configuration.getClassResolver().resolveDescriptor(descriptor);
    }

    private String getRealAnnotationDescriptor(String descriptor) {
        return configuration.isJvmAnnotationDesc(descriptor)
            ? descriptor : getDJVMSyntheticDescriptor(mapToSandbox(descriptor));
    }

    private String getSandboxAnnotationDescriptor(String descriptor) {
        return configuration.isJvmAnnotationDesc(descriptor)
            ? descriptor : mapToSandbox(descriptor);
    }

    private Type getSandboxAnnotationDescriptor(@NotNull Type type) {
        return Type.getType(getSandboxAnnotationDescriptor(type.getDescriptor()));
    }

    @Override
    public void visit(String name, Object value) {
        /*
         * Handles primitive values, strings and class descriptors.
         */
        super.visit(name, (value instanceof Type) ? getSandboxAnnotationDescriptor((Type) value) : value);
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
        /*
         * Synthetic sandbox annotations must store sandbox.java.lang.Enum
         * values as strings because user-defined enum types are untrusted.
         */
        av.visit(name, value);
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        AnnotationVisitor visit = super.visitArray(name);
        return (visit != null) ? new AnnotationTransformer(api, visit, configuration) : null;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
        AnnotationVisitor visit = super.visitAnnotation(name, getRealAnnotationDescriptor(descriptor));
        return (visit != null) ? new AnnotationTransformer(api, visit, configuration) : null;
    }
}
