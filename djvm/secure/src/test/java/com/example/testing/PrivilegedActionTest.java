package com.example.testing;

import net.corda.djvm.TypedTaskFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class PrivilegedActionTest extends TestBase {
    @Test
    void testCheckedAction() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> WithJava.run(taskFactory, PrivilegedCheckedTask.class, null));
                assertThat(ex)
                    .hasCauseExactlyInstanceOf(RuntimeException.class);
                assertThat(ex.getCause())
                    .hasMessageStartingWith("sandbox.java.security.AccessControlException -> ")
                    .hasMessageContainingAll(
                        "access denied",
                        "\"java.lang.RuntimePermission\"",
                        "\"closeClassLoader\""
                    );
            } catch (Exception e) {
                fail(e);
            }
        });
    }
}
