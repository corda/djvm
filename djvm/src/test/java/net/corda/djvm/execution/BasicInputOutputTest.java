package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class BasicInputOutputTest extends TestBase {
    private static final String MESSAGE = "Hello World!";
    private static final Long BIG_NUMBER = 123456789000L;

    BasicInputOutputTest() {
        super(JAVA);
    }

    @Test
    void testBasicInput() {
        sandbox(ctx -> {
            try {
                Function<? super Object, ?> inputTask = ctx.getClassLoader().createBasicInput();
                Object sandboxObject = inputTask.apply(MESSAGE);
                assertEquals("sandbox.java.lang.String", sandboxObject.getClass().getName());
                assertEquals(MESSAGE, sandboxObject.toString());
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testBasicOutput() {
        sandbox(ctx -> {
            try {
                Function<? super Object, ?> inputTask = ctx.getClassLoader().createBasicInput();
                Object sandboxObject = inputTask.apply(BIG_NUMBER);

                Function<? super Object, ?> outputTask = ctx.getClassLoader().createBasicOutput();
                Object output = outputTask.apply(sandboxObject);
                assertThat(output)
                    .isExactlyInstanceOf(Long.class)
                    .isEqualTo(BIG_NUMBER);
            } catch (Exception e) {
                fail(e);
            }
        });
    }
}
