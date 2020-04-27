package net.corda.djvm

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.security.Security

class BouncyCastle : BeforeAllCallback, AfterAllCallback {
    private var isRemovable = false

    override fun beforeAll(context: ExtensionContext?) {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.insertProviderAt(BouncyCastleProvider(), 1)
            isRemovable = true
        }
    }

    override fun afterAll(context: ExtensionContext?) {
        if (isRemovable) {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        }
    }
}