package net.corda.djvm.assertions

import net.corda.djvm.messages.MessageCollection
import net.corda.djvm.messages.Severity
import org.assertj.core.api.Assertions.assertThat

@Suppress("unused")
class AssertiveMessages(private val messages: MessageCollection) {

    fun isEmpty() {
        hasErrorCount(0)
        hasWarningCount(0)
        hasInfoCount(0)
    }

    fun hasErrorCount(count: Int): AssertiveMessages {
        assertThat(messages.statistics[Severity.ERROR])
                .`as`("Number of errors")
                .withFailMessage(formatMessages(Severity.ERROR, count))
                .isEqualTo(count)
        return this
    }

    fun hasWarningCount(count: Int): AssertiveMessages {
        assertThat(messages.statistics[Severity.WARNING])
                .`as`("Number of warnings")
                .withFailMessage(formatMessages(Severity.WARNING, count))
                .isEqualTo(count)
        return this
    }

    fun hasInfoCount(count: Int): AssertiveMessages {
        assertThat(messages.statistics[Severity.INFORMATIONAL])
                .`as`("Number of informational messages")
                .withFailMessage(formatMessages(Severity.INFORMATIONAL, count))
                .isEqualTo(count)
        return this
    }

    fun hasTraceCount(count: Int): AssertiveMessages {
        assertThat(messages.statistics[Severity.TRACE])
                .`as`("Number of trace messages")
                .withFailMessage(formatMessages(Severity.TRACE, count))
                .isEqualTo(count)
        return this
    }

    fun withMessage(message: String): AssertiveMessages {
        assertThat(messages.sorted())
                .`as`("Has message: $message")
                .anySatisfy {
                    assertThat(it.message).contains(message)
                }
        return this
    }

    private fun formatMessages(severity: Severity, expectedCount: Int): String {
        val filteredMessages = messages.sorted().filter { it.severity == severity }
        return StringBuilder().apply {
            append("Found ${filteredMessages.count()} message(s) of severity $severity, ")
            appendln("expected $expectedCount:")
            for (message in filteredMessages) {
                appendln(" - $message")
            }
        }.toString()
    }

}
