package net.corda.djvm

import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.code.DefinitionProvider
import net.corda.djvm.code.EMIT_TRACING
import net.corda.djvm.code.Emitter
import net.corda.djvm.execution.ExecutionProfile
import net.corda.djvm.rewiring.SandboxClassLoader
import net.corda.djvm.rules.Rule
import net.corda.djvm.rules.implementation.*
import net.corda.djvm.rules.implementation.instrumentation.*

/**
 * Configuration to use for the deterministic sandbox.
 *
 * @property rules The rules to apply during the analysis phase.
 * @property emitters The code emitters / re-writers to apply to all loaded classes.
 * @property definitionProviders The meta-data providers to apply to class and member definitions.
 * @property executionProfile The execution profile to use in the sandbox.
 * @property analysisConfiguration The configuration used in the analysis of classes.
 * @property parentClassLoader The [SandboxClassLoader] that this sandbox will use as a parent.
 */
class SandboxConfiguration private constructor(
        val rules: List<Rule>,
        val emitters: List<Emitter>,
        val definitionProviders: List<DefinitionProvider>,
        val executionProfile: ExecutionProfile,
        val analysisConfiguration: AnalysisConfiguration,
        val parentClassLoader: SandboxClassLoader?
) {
    companion object {
        val ALL_RULES: List<Rule> = listOf(
            AlwaysUseNonSynchronizedMethods,
            AlwaysUseStrictFloatingPointArithmetic,
            DisallowDynamicInvocation,
            DisallowOverriddenSandboxPackage,
            DisallowUnsupportedApiVersions
        )

        val ALL_DEFINITION_PROVIDERS: List<DefinitionProvider> = listOf(
            AlwaysInheritFromSandboxedObject,
            AlwaysUseNonSynchronizedMethods,
            AlwaysUseStrictFloatingPointArithmetic,
            StaticConstantRemover,
            StubOutFinalizerMethods,
            StubOutNativeMethods,
            StubOutReflectionMethods
        )

        val ALL_EMITTERS: List<Emitter> = listOf(
            AlwaysInheritFromSandboxedObject,
            AlwaysUseExactMath,
            ArgumentUnwrapper,
            DisallowCatchingBlacklistedExceptions,
            DisallowNonDeterministicMethods,
            HandleExceptionUnwrapper,
            IgnoreBreakpoints,
            IgnoreSynchronizedBlocks,
            ReturnTypeWrapper,
            RewriteClassMethods,
            RewriteObjectMethods,
            StringConstantWrapper,
            ThrowExceptionWrapper,
            TraceAllocations,
            TraceInvocations,
            TraceJumps,
            TraceThrows
        )

        /**
         * Default configuration for the deterministic sandbox.
         */
        @JvmField
        val DEFAULT = of()

        /**
         * Configuration with no emitters, rules, meta-data providers or runtime thresholds.
         */
        @JvmField
        val EMPTY = of(
                ExecutionProfile.UNLIMITED, emptyList(), emptyList(), emptyList()
        )

        /**
         * Create a sandbox configuration where one or more properties deviates from the default.
         */
        fun of(
                profile: ExecutionProfile = ExecutionProfile.DEFAULT,
                rules: List<Rule> = ALL_RULES,
                emitters: List<Emitter>? = null,
                definitionProviders: List<DefinitionProvider> = ALL_DEFINITION_PROVIDERS,
                enableTracing: Boolean = true,
                analysisConfiguration: AnalysisConfiguration = AnalysisConfiguration.createRoot(),
                parentClassLoader: SandboxClassLoader? = null
        ) = SandboxConfiguration(
                executionProfile = profile,
                rules = rules,
                emitters = (emitters ?: ALL_EMITTERS).filter {
                    enableTracing || it.priority > EMIT_TRACING
                },
                definitionProviders = definitionProviders,
                analysisConfiguration = analysisConfiguration,
                parentClassLoader = parentClassLoader
        )
    }
}
