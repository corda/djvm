package com.example.testing

import net.corda.core.contracts.Attachment
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import java.io.InputStream
import java.security.PublicKey
import java.util.function.Function

class AttachmentStreamer : Function<Array<Any>, ByteArray> {
    override fun apply(inputs: Array<Any>): ByteArray {
        @Suppress("unchecked_cast")
        val attachment = SandboxAttachment(
            id = inputs[0] as SecureHash,
            size = inputs[1] as Int,
            attachment = inputs[2],
            streamer = inputs[3] as Function<in Any, out InputStream>
        )
        return attachment.open().readBytes()
    }
}

private class SandboxAttachment(
    override val id: SecureHash,
    override val size: Int,
    private val attachment: Any,
    private val streamer: Function<Any, out InputStream>
) : Attachment {
    @Suppress("OverridingDeprecatedMember")
    override val signers: List<Party> = emptyList()

    override val signerKeys: List<PublicKey> = emptyList()

    override fun open(): InputStream {
        return streamer.apply(attachment)
    }
}
