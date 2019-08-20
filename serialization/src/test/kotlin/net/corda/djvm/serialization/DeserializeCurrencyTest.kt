package net.corda.djvm.serialization

import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.deserialize
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
        val data = SerializedBytes<Any>(currency.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxCurrency = data.deserialize()

            val executor = createExecutorFor(classLoader)
            val result = executor.apply(
                classLoader.loadClassForSandbox(ShowCurrency::class.java).newInstance(),
                sandboxCurrency
            ) ?: fail("Result cannot be null")

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
