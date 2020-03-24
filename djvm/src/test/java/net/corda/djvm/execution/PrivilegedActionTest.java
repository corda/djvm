package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class PrivilegedActionTest extends TestBase {
    PrivilegedActionTest() {
        super(JAVA);
    }

    @Test
    void testPrivilegedException() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuntimeException ex = assertThrows(RuntimeException.class,
                        () -> WithJava.run(taskFactory, PrivilegedExceptionTask.class, "Got Root!"));
                assertThat(ex)
                    .isExactlyInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(PrivilegedActionException.class)
                    .hasMessage(null);
                assertThat(ex.getCause())
                    .isExactlyInstanceOf(PrivilegedActionException.class)
                    .hasCauseInstanceOf(Exception.class)
                    .hasMessage(null);
                assertThat(ex.getCause().getCause())
                    .hasMessage("sandbox.net.corda.djvm.execution.MyExampleException -> Got Root!");
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class PrivilegedExceptionTask implements Function<String, String> {
        @Override
        public String apply(String message) {
            try {
                return AccessController.doPrivileged(new DoExceptionAction(message));
            } catch (PrivilegedActionException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    public static class DoExceptionAction implements PrivilegedExceptionAction<String> {
        private final String message;

        DoExceptionAction(String message) {
            this.message = message;
        }

        @Override
        public String run() throws Exception {
            throw new MyExampleException(message);
        }
    }

    @Test
    void testCheckedAction() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String result = WithJava.run(taskFactory, PrivilegedCheckedTask.class, null);
                assertThat(result).isEqualTo("SUCCESS");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class PrivilegedCheckedTask implements Function<String, String> {
        @Override
        public String apply(String s) {
            return AccessController.doPrivileged(new DoCheckedAction());
        }
    }

    public static class DoCheckedAction implements PrivilegedAction<String> {
        @Override
        public String run() {
            getClass().getClassLoader();
            return "SUCCESS";
        }
    }
}
