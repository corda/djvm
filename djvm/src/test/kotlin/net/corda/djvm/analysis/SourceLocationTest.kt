package net.corda.djvm.analysis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SourceLocationTest {

    @Test
    fun `can derive description of source location without source`() {
        val location = SourceLocation(className = "net/foo/Bar")
        assertThat(location.format())
                .contains("net/foo/Bar")
        assertThat(location.copy(memberName = "baz").format())
                .contains("net/foo/Bar")
        assertThat(location.copy(memberName = "baz", lineNumber = 15).format())
                .contains("net/foo/Bar")
                .contains("line 15")
        assertThat(location.copy(memberName = "baz", descriptor = "(I)V", lineNumber = 15).format())
                .contains("net/foo/Bar")
                .contains("line 15")
                .contains("baz(Integer)")
    }

    @Test
    fun `can derive description of source location with source`() {
        val location = SourceLocation(className = "net/foo/Bar", sourceFile = "Bar.kt")
        assertThat(location.format())
                .contains("Bar.kt")
        assertThat(location.copy(memberName = "baz").format())
                .contains("Bar.kt")
        assertThat(location.copy(memberName = "baz", lineNumber = 15).format())
                .contains("Bar.kt")
                .contains("line 15")
        assertThat(location.copy(memberName = "baz", descriptor = "(I)V", lineNumber = 15).format())
                .contains("Bar.kt")
                .contains("line 15")
                .contains("baz(Integer)")
    }

}
