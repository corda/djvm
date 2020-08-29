package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import net.corda.djvm.api.RuleViolationError;
import org.junit.jupiter.api.Test;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class JavaAccessControllerTest extends TestBase {
    JavaAccessControllerTest() {
        super(JAVA);
    }

    @Test
    void testPrivilegedActionByMethodReference() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, DoPrivilegedAction.class, null)
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class DoPrivilegedAction implements Function<String, Object> {
        @Override
        public Object apply(String unused) {
            Function<PrivilegedAction<Object>, Object> action = AccessController::doPrivileged;
            return action.apply(Object::new);
        }
    }
}