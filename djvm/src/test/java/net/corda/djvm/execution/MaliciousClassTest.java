package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.WithJava;
import net.corda.djvm.rewiring.SandboxClassLoadingException;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static net.corda.djvm.messages.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MaliciousClassTest extends TestBase {
    MaliciousClassTest() {
        super(JAVA);
    }

    @Test
    void testImplementingToDJVMString() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<String, String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            Throwable ex = assertThrows(SandboxClassLoadingException.class, () -> WithJava.run(executor, EvilToString.class, ""));
            assertThat(ex)
                .hasMessageContaining("Class is not allowed to implement toDJVMString()");
            return null;
        });
    }

    public static class EvilToString implements Function<String, String> {
        @Override
        public String apply(String s) {
            return toString();
        }

        @SuppressWarnings("unused")
        public String toDJVMString() {
            throw new IllegalStateException("MUHAHAHAHAHAHA!!!");
        }
    }

    @Test
    void testImplementingFromDJVM() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<Object, Object> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            Throwable ex = assertThrows(SandboxClassLoadingException.class, () -> WithJava.run(executor, EvilFromDJVM.class, null));
            assertThat(ex)
                .hasMessageContaining("Class is not allowed to implement fromDJVM()");
            return null;
        });
    }

    public static class EvilFromDJVM implements Function<Object, Object> {
        @Override
        public Object apply(Object obj) {
            return this;
        }

        @SuppressWarnings("unused")
        protected Object fromDJVM() {
            throw new IllegalStateException("MUHAHAHAHAHAHA!!!");
        }
    }
}
