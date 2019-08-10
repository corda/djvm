package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static net.corda.djvm.messages.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class BasicInputOutputTest extends TestBase {
    private static final String MESSAGE = "Hwllo World!";
    private static final Long BIG_NUMBER = 123456789000L;

    BasicInputOutputTest() {
        super(JAVA);
    }

    @Test
    void testBasicInput() {
        parentedSandbox(WARNING, false, ctx -> {
            try {
                Function<Object, ?> inputTask = ctx.getClassLoader().createBasicInput();
                Object sandboxObject = inputTask.apply(MESSAGE);
                assertEquals("sandbox.java.lang.String", sandboxObject.getClass().getName());
                assertEquals(MESSAGE, sandboxObject.toString());
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testBasicOutput() {
        parentedSandbox(WARNING, false, ctx -> {
            try {
                Function<Object, ?> inputTask = ctx.getClassLoader().createBasicInput();
                Object sandboxObject = inputTask.apply(BIG_NUMBER);

                Function<Object, ?> outputTask = ctx.getClassLoader().createBasicOutput();
                Object output = outputTask.apply(sandboxObject);
                assertThat(output)
                    .isExactlyInstanceOf(Long.class)
                    .isEqualTo(BIG_NUMBER);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }
}
