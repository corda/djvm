package net.corda.djvm

import net.corda.djvm.analysis.AnalysisConfiguration
import net.corda.djvm.analysis.AnalysisConfiguration.ChildBuilder
import net.corda.djvm.code.DefinitionProvider
import net.corda.djvm.code.EMIT_TRACING
import net.corda.djvm.code.Emitter
import net.corda.djvm.execution.ExecutionProfile
import net.corda.djvm.rewiring.ByteCodeCache
import net.corda.djvm.rules.Rule
import net.corda.djvm.rules.implementation.*
import net.corda.djvm.rules.implementation.instrumentation.*
import net.corda.djvm.source.UserSource
import java.util.Collections.unmodifiableList
import java.util.function.Consumer

/**
 * Configuration to use for the deterministic sandbox. It also caches the bytecode
 * for the sandbox classes that have been generated according to these rules.
 *
 * @property rules The rules to apply during the analysis phase.
 * @property emitters The code emitters / re-writers to apply to all loaded classes.
 * @property definitionProviders The meta-data providers to apply to class and member definitions.
 * @property executionProfile The execution profile to use in the sandbox.
 * @property analysisConfiguration The configuration used in the analysis of classes.
 * @property byteCodeCache A cache of bytecode generated using these rules, emitters and definition providers.
 */
class SandboxConfiguration private constructor(
    val rules: List<Rule>,
    val emitters: List<Emitter>,
    val definitionProviders: List<DefinitionProvider>,
    val executionProfile: ExecutionProfile,
    val analysisConfiguration: AnalysisConfiguration,
    val byteCodeCache: ByteCodeCache
) {
    /**
     * Creates a child [SandboxConfiguration] with this instance as its parent.
     * @param userSource Source for additional classes to be included in the new sandbox.
     * @param configure A callback function so that we can configure the [ChildBuilder].
     */
    fun createChild(userSource: UserSource, configure: Consumer<ChildBuilder>): SandboxConfiguration {
        return SandboxConfiguration(
            rules = rules,
            emitters = emitters,
            definitionProviders = definitionProviders,
            executionProfile = executionProfile,
            analysisConfiguration = with(analysisConfiguration.createChild(userSource)) {
                configure.accept(this)
                build()
            },
            byteCodeCache = ByteCodeCache(byteCodeCache)
        )
    }

    /**
     * Creates a child [SandboxConfiguration] with this instance as its parent.
     * @param userSource Source for additional classes to be included in the new sandbox.
     */
    fun createChild(userSource: UserSource): SandboxConfiguration {
        return createChild(userSource, Consumer {})
    }

    companion object {
        @JvmField
        val ALL_RULES: List<Rule> = unmodifiableList(listOf(
            AlwaysUseNonSynchronizedMethods,
            AlwaysUseStrictFloatingPointArithmetic,
            DisallowOverriddenSandboxPackage,
            DisallowSandboxInstructions,
            DisallowSandboxMethods,
            DisallowUnsupportedApiVersions
        ))

        @JvmField
        val ALL_DEFINITION_PROVIDERS: List<DefinitionProvider> = unmodifiableList(listOf(
            AlwaysInheritFromSandboxedObject,
            AlwaysUseNonSynchronizedMethods,
            AlwaysUseStrictFloatingPointArithmetic,
            StaticConstantRemover,
            StubOutFinalizerMethods,
            StubOutNativeMethods,
            StubOutReflectionMethods
        ))

        @JvmField
        val ALL_EMITTERS: List<Emitter> = unmodifiableList(listOf(
            AlwaysInheritFromSandboxedObject,
            AlwaysUseExactMath,
            ArgumentUnwrapper,
            DisallowDynamicInvocation,
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
        ))

        /**
         * Create a sandbox configuration where one or more properties deviates from the default.
         */
        fun of(
            profile: ExecutionProfile = ExecutionProfile.DEFAULT,
            rules: List<Rule> = ALL_RULES,
            emitters: List<Emitter>? = null,
            definitionProviders: List<DefinitionProvider> = ALL_DEFINITION_PROVIDERS,
            enableTracing: Boolean = true,
            analysisConfiguration: AnalysisConfiguration
        ) = SandboxConfiguration(
                executionProfile = profile,
                rules = rules,
                emitters = (emitters ?: ALL_EMITTERS).filter {
                    enableTracing || it.priority > EMIT_TRACING
                },
                definitionProviders = definitionProviders,
                analysisConfiguration = analysisConfiguration,
                byteCodeCache = ByteCodeCache.createFor(analysisConfiguration)
        )

        /**
         * Create a fresh [SandboxConfiguration] that contains all rules,
         * emitters and definition providers.
         */
        fun createFor(
            analysisConfiguration: AnalysisConfiguration,
            profile: ExecutionProfile,
            enableTracing: Boolean
        ): SandboxConfiguration {
            return of(
                profile = profile,
                enableTracing = enableTracing,
                analysisConfiguration = analysisConfiguration
            )
        }
    }
}
