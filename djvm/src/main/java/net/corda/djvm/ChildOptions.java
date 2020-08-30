package net.corda.djvm;

import net.corda.djvm.analysis.AnalysisOptions;

/**
 * Unify {@link AnalysisOptions} and {@link ConfigurationOptions} into a single
 * interface that {@link SandboxConfiguration#createChild} can consume.
 */
public interface ChildOptions extends AnalysisOptions, ConfigurationOptions {
}
