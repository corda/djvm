package net.corda.djvm.analysis;

import net.corda.djvm.messages.Severity;

import java.lang.annotation.Annotation;

public interface AnalysisOptions {
    void setMinimumSeverityLevel(Severity level);
    void setSandboxOnlyAnnotations(Iterable<String> annotations);
    void setVisibleAnnotations(Iterable<Class<? extends Annotation>> annotations);
}
