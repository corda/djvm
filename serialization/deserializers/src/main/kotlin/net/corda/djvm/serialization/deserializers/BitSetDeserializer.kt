package net.corda.djvm.serialization.deserializers

import net.corda.serialization.internal.amqp.custom.BitSetSerializer.BitSetProxy
import java.util.*
import java.util.function.Function

class BitSetDeserializer : Function<BitSetProxy, BitSet> {
    override fun apply(proxy: BitSetProxy): BitSet {
        return BitSet.valueOf(proxy.bytes)
    }
}
