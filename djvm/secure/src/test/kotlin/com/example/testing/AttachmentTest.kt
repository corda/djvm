package com.example.testing

import net.corda.core.contracts.Attachment
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.serialization.internal._contextSerializationEnv
import net.corda.core.serialization.serialize
import net.corda.serialization.djvm.createSandboxSerializationEnv
import net.corda.serialization.djvm.deserializeFor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.PublicKey
import java.util.function.Function

@ExtendWith(LocalSerialization::class)
class AttachmentTest : TestBase() {
    companion object {
        const val SECRET_MESSAGE = "Very Secret Message!!!"
    }

    @Test
    fun `test streaming attachment`() {
        val attachment = BigAttachment(
            id = SecureHash.allOnesHash,
            data = SECRET_MESSAGE.toByteArray()
        )
        val idData = attachment.id.serialize()

        sandbox {
            _contextSerializationEnv.set(createSandboxSerializationEnv(classLoader))
            val sandboxId = idData.deserializeFor(classLoader)

            val taskFactory = classLoader.createRawTaskFactory().compose(classLoader.createSandboxFunction())
            val streamTask = taskFactory.apply(AttachmentStreamer::class.java)
            val basicInput = classLoader.createBasicInput()

            val openAttachment = classLoader.createForImport(
                Function(Attachment::open).andThen(basicInput)
            )

            val result = streamTask.apply(arrayOf(
                sandboxId,
                basicInput.apply(attachment.size),
                attachment,
                openAttachment
            )) as ByteArray
            assertEquals(SECRET_MESSAGE, String(result))
        }
    }
}

class BigAttachment(
    override val id: SecureHash,
    private val data: ByteArray
) : Attachment {
    @Suppress("OverridingDeprecatedMember")
    override val signers: List<Party> = emptyList()

    override val signerKeys: List<PublicKey> = emptyList()

    override fun open(): InputStream {
        return ByteArrayInputStream(data)
    }

    override val size: Int = data.size
}
