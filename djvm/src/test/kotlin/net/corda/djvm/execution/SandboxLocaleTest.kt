package net.corda.djvm.execution

import net.corda.djvm.SandboxType.KOTLIN
import net.corda.djvm.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.Locale
import java.util.function.Function

class SandboxLocaleTest : TestBase(KOTLIN) {
    @ParameterizedTest
    @CsvSource("en,en,''", "en-GB,en,GB", "en-US,en,US", "en-CA,en,CA", "en-AU,en,AU")
    fun `test loading locales`(tagName: String, language: String, country: String) = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(LookupLocale::class.java)
            .apply(tagName)
        assertThat(result).isEqualTo(arrayOf(language, country))
    }

    class LookupLocale : Function<String, Array<String>> {
        override fun apply(tagName: String): Array<String> {
            return Locale.forLanguageTag(tagName).let {
                arrayOf(it.language, it.country)
            }
        }
    }

    @Test
    fun `test locale languages`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(GetAllLocaleLanguages::class.java)
            .apply("")
        assertThat(result)
            .hasSize(188)
            .contains("en", "fr", "hu", "it", "ru", "zh")
    }

    class GetAllLocaleLanguages : Function<String, Array<String>> {
        override fun apply(input: String): Array<String> {
            return Locale.getISOLanguages().sortedArray()
        }
    }

    @Test
    fun `test locale countries`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(GetAllLocaleCountries::class.java)
            .apply("")
        assertThat(result)
            .hasSize(250)
            .contains("AU", "DE", "FR", "GB", "MX", "US")
    }

    class GetAllLocaleCountries : Function<String, Array<String>> {
        override fun apply(input: String): Array<String> {
            return Locale.getISOCountries().sortedArray()
        }
    }

    @Test
    fun `test default locale`() = sandbox {
        val taskFactory = classLoader.createTypedTaskFactory()
        val result = taskFactory.create(GetDefaultLocale::class.java)
            .apply("")
        assertThat(result).isEqualTo("en")
    }

    class GetDefaultLocale : Function<String, String> {
        override fun apply(input: String): String {
            return Locale.getDefault().toString()
        }
    }
}