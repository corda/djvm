package com.example.testing;

import net.corda.djvm.TypedTaskFactory;
import org.junit.jupiter.api.Test;

import static com.example.testing.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JavaSandboxTest extends TestBase {
    private static final long BIG_NUMBER = 1234L;

    JavaSandboxTest() {
        super(JAVA);
    }

    @Test
    void testGoodTask() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String result = WithJava.run(taskFactory, JavaTask.class, "Hello World!");
                assertEquals("Sandbox says: 'Hello World!'", result);
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testBadTask() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Throwable ex = assertThrows(NoSuchMethodError.class, () -> WithJava.run(taskFactory, BadJavaTask.class, BIG_NUMBER));
                assertThat(ex)
                    .isExactlyInstanceOf(NoSuchMethodError.class)
                    .hasMessageContaining("sandbox.java.lang.System.currentTimeMillis()")
                    .hasMessageFindingMatch("(long sandbox\\.|\\.currentTimeMillis\\(\\)J)+");
            } catch(Exception e) {
                fail(e);
            }
        });
    }
}
