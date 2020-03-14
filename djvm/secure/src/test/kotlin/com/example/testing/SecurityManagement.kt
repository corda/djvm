package com.example.testing

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class SecurityManagement : BeforeAllCallback, AfterAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        System.setSecurityManager(SecurityManager())
    }

    override fun afterAll(context: ExtensionContext) {
        System.setSecurityManager(null)
    }
}

