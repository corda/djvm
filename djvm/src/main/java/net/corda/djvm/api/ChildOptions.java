package net.corda.djvm.api;

import net.corda.djvm.SandboxConfiguration;

/**
 * Unify {@link AnalysisOptions} and {@link ConfigurationOptions} into a single
 * interface that {@link SandboxConfiguration#createChild} can consume.
 */
public interface ChildOptions extends AnalysisOptions, ConfigurationOptions {
}
