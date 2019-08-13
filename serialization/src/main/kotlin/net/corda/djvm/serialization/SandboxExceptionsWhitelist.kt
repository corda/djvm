package net.corda.djvm.serialization

import net.corda.core.serialization.ClassWhitelist

class SandboxExceptionsWhitelist : ClassWhitelist {
    companion object {
        private val packageName = "^sandbox\\.(?:java|kotlin)(?:[.]|$)".toRegex()
    }

    override fun hasListed(type: Class<*>): Boolean {
        return packageName.containsMatchIn(type.name.dropLast(type.simpleName.length + 1))
    }
}
