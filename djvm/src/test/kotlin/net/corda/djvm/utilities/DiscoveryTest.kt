package net.corda.djvm.utilities

import io.github.classgraph.ClassGraph
import net.corda.djvm.SandboxConfiguration.Companion.ALL_DEFINITION_PROVIDERS
import net.corda.djvm.SandboxConfiguration.Companion.ALL_EMITTERS
import net.corda.djvm.SandboxConfiguration.Companion.ALL_RULES
import net.corda.djvm.code.DefinitionProvider
import net.corda.djvm.code.Emitter
import net.corda.djvm.rules.Rule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier

/**
 * Verify that we have installed all of the DJVM's [Rule], [DefinitionProvider] and [Emitter] objects.
 */
class DiscoveryTest {
    companion object {
        const val FORBIDDEN_CLASS_MASK = (Modifier.STATIC or Modifier.ABSTRACT or Modifier.PRIVATE or Modifier.PROTECTED)
    }

    /**
     * Get an instance of each concrete class that implements interface or class [T].
     */
    private inline fun <reified T> find(): List<Class<T>> {
        return ClassGraph()
            .acceptPaths("net/corda/djvm")
            .enableAllInfo()
            .scan()
            .use { it.getClassesImplementing(T::class.java.name).loadClasses(T::class.java) }
            .filter { it.modifiers and FORBIDDEN_CLASS_MASK == 0 }
    }

    @Test
    fun `can discover rules automatically`() {
        val rules = find<Rule>()
        assertThat(rules)
            .isNotEmpty
            .hasSameSizeAs(ALL_RULES)
        assertThat(ALL_RULES.map(Rule::javaClass))
            .containsExactlyInAnyOrderElementsOf(rules)
    }

    @Test
    fun `can discover definition providers automatically`() {
        val definitionProviders = find<DefinitionProvider>()
        assertThat(definitionProviders)
            .isNotEmpty
            .hasSameSizeAs(ALL_DEFINITION_PROVIDERS)
        assertThat(ALL_DEFINITION_PROVIDERS.map(DefinitionProvider::javaClass))
            .containsExactlyInAnyOrderElementsOf(definitionProviders)
    }

    @Test
    fun `can discover emitters automatically`() {
        val emitters = find<Emitter>()
        assertThat(emitters)
            .isNotEmpty
            .hasSameSizeAs(ALL_EMITTERS)
        assertThat(ALL_EMITTERS.map(Emitter::javaClass))
            .containsExactlyInAnyOrderElementsOf(emitters)
    }

}
