package com.example.testing

import com.example.testing.SandboxType.KOTLIN
import com.example.testing.tokens.FungibleTask
import com.example.testing.tokens.House
import com.example.testing.tokens.HouseContract
import com.example.testing.tokens.NonFungibleTask
import com.example.testing.tokens.RUB
import com.example.testing.tokens.ShowHouse
import com.example.testing.tokens.ShowHouseContract
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.utilities.heldBy
import com.r3.corda.lib.tokens.money.GBP
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.serialization.SerializedBytes
import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.serialization.djvm.createSandboxSerializationEnv
import net.corda.serialization.djvm.deserializeFor
import net.corda.testing.core.TestIdentity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.fail

@ExtendWith(LocalSerialization::class)
class TokenTest : TestBase(KOTLIN) {
    companion object {
        val ISSUER = TestIdentity(CordaX500Name("ISSUER", "London", "GB"))
        val ALICE = TestIdentity(CordaX500Name("ALICE", "London", "GB"))
    }

    @Test
    fun `test house`() {
        val house = House(
            "24 Leinster Gardens, Bayswater, London",
            1_000_000.GBP,
            listOf(ALICE.party),
            2,
            UniqueIdentifier()
        )
        val data = SerializedBytes<Any>(house.serialize().bytes)

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxHouse = data.deserializeFor(classLoader)
            assertEquals("sandbox.${house::class.java.name}", sandboxHouse::class.java.name)

            val taskFactory = classLoader.createRawTaskFactory()
            @Suppress("unchecked_cast")
            val result = taskFactory.compose(classLoader.createSandboxFunction())
                .apply(ShowHouse::class.java)
                .andThen(classLoader.createBasicOutput())
                .apply(sandboxHouse) as? String ?: fail("Unexpected result type")
            assertThat(result).isEqualTo(house.toString())
        }
    }

    @Test
    fun `test house contract`() {
        val contract = HouseContract()
        val data = contract.serialize()

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxContract = data.deserializeFor(classLoader)
            assertEquals("sandbox.${contract::class.java.name}", sandboxContract::class.java.name)

            val taskFactory = classLoader.createRawTaskFactory()
            @Suppress("unchecked_cast")
            val result = taskFactory.compose(classLoader.createSandboxFunction())
                .apply(ShowHouseContract::class.java)
                .andThen(classLoader.createBasicOutput())
                .apply(sandboxContract) as? String ?: fail("Unexpected result type")
            assertThat(result).isEqualTo("sandbox.com.example.testing.tokens.HouseContract:toString()")
        }
    }

    @Test
    fun `test fungible token`() {
        val ruble = RUB issuedBy ISSUER.party
        val tokenData = ruble.serialize()
        val fungibleToken = FungibleToken(10 of ruble, ALICE.party)
        val fungibleData = fungibleToken.serialize()

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))
            val sandboxToken = tokenData.deserializeFor(classLoader)
            assertEquals("sandbox.${ruble::class.java.name}", sandboxToken::class.java.name)

            val sandboxFungible = fungibleData.deserializeFor(classLoader)

            val taskFactory = classLoader.createRawTaskFactory()
            @Suppress("unchecked_cast")
            val result = taskFactory.compose(classLoader.createSandboxFunction())
                .apply(FungibleTask::class.java)
                .andThen(classLoader.createBasicOutput())
                .apply(sandboxFungible) as? Array<String> ?: fail("Unexpected result type")
            assertThat(result).containsExactly(
                *FungibleTask().apply(fungibleToken)
            )
        }
    }

    @Test
    fun `test non-fungible token`() {
        val house = House(
            "24 Leinster Gardens, Bayswater, London",
            1_000_000.GBP,
            listOf(ISSUER.party),
            linearId = UniqueIdentifier()
        )
        val housePointer: TokenPointer<House> = house.toPointer()
        val issuedHouse: IssuedTokenType = housePointer issuedBy ISSUER.party
        val nonFungibleToken = issuedHouse heldBy ALICE.party
        val nonFungibleData = nonFungibleToken.serialize()

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))

            val sandboxNonFungible = nonFungibleData.deserializeFor(classLoader)

            val taskFactory = classLoader.createRawTaskFactory()
            @Suppress("unchecked_cast")
            val result = taskFactory.compose(classLoader.createSandboxFunction())
                .apply(NonFungibleTask::class.java)
                .andThen(classLoader.createBasicOutput())
                .apply(sandboxNonFungible) as? Array<String> ?: fail("Unexpected result type")
            assertThat(result).containsExactlyInAnyOrder(
                nonFungibleToken.holder.toString(),
                nonFungibleToken.linearId.toString(),
                nonFungibleToken.toString().replace("class com.example.", "class sandbox.com.example."),
                nonFungibleToken.tokenType.toString().replace("class com.example.", "class sandbox.com.example."),
                null
            )
        }
    }
}
