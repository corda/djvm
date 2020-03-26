package com.example.testing.tokens

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import net.corda.core.contracts.Amount.Companion.zero
import net.corda.core.contracts.Contract
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction

@CordaSerializable
class HouseContract : EvolvableTokenContract(), Contract {
    override fun additionalCreateChecks(tx: LedgerTransaction) {
        // Not much to do for this example token.
        val newHouse = tx.outputStates.single() as House
        newHouse.apply {
            require(valuation > zero(valuation.token)) { "Valuation must be greater than zero." }
        }
    }

    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        val oldHouse = tx.inputStates.single() as House
        val newHouse = tx.outputStates.single() as House
        require(oldHouse.address == newHouse.address) { "The address cannot change." }
        require(newHouse.valuation > zero(newHouse.valuation.token)) { "Valuation must be greater than zero." }
    }

    override fun toString(): String {
        return "${this::class.java.name}:toString()"
    }
}
