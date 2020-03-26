package com.example.testing.tokens

import java.util.function.Function

class ShowHouse : Function<House, String> {
    override fun apply(house: House): String {
        return house.toString()
    }
}

class ShowHouseContract : Function<HouseContract, String> {
    override fun apply(contract: HouseContract): String {
        return contract.toString()
    }
}
