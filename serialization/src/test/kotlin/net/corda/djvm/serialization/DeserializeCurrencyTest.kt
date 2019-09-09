package net.corda.djvm.serialization

import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.djvm.serialization.SandboxType.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail
import java.util.*
import java.util.function.Function

@ExtendWith(LocalSerialization::class)
class DeserializeCurrencyTest : TestBase(KOTLIN) {
    @Test
    fun `test deserializing currency`() {
        val currency = CurrencyData(Currency.getInstance("GBP"))
        val data = currency.serialize()

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxCurrency = data.deserializeFor(classLoader)

            val taskFactory = classLoader.createRawTaskFactory()
            val showCurrency = classLoader.createTaskFor(taskFactory, ShowCurrency::class.java)
            val result = showCurrency.apply(sandboxCurrency) ?: fail("Result cannot be null")

            assertEquals(ShowCurrency().apply(currency), result.toString())
            assertEquals(SANDBOX_STRING, result::class.java.name)
        }
    }

    class ShowCurrency : Function<CurrencyData, String> {
        override fun apply(data: CurrencyData): String {
            return with(data) {
                "Currency: $currency"
            }
        }
    }
}

@CordaSerializable
data class CurrencyData(val currency: Currency)
