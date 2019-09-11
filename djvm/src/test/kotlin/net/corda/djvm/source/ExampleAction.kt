package net.corda.djvm.source

import net.corda.djvm.Action

class ExampleAction : Action<String, String> {
    override fun action(input: String) = input
}
