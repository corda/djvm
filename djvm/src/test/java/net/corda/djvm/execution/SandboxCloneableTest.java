package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static net.corda.djvm.messages.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SandboxCloneableTest extends TestBase {
    @Test
    void testCloningInsideSandbox() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<String, String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult success = WithJava.run(executor, CloningMachine.class, "Jango Fett");
            assertThat(success.getResult()).isEqualTo("Jango Fett");
            return null;
        });
    }

    public static class Soldier implements Cloneable {
        private final String name;

        Soldier(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public Soldier clone() {
            try {
                return (Soldier) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    public static class CloningMachine implements Function<String, String> {
        @Override
        public String apply(String subjectName) {
            Soldier soldier = new Soldier(subjectName);
            return soldier.clone().getName();
        }
    }

    @Test
    void testFailedCloningInsideSandbox() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<String, String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            Throwable throwable = assertThrows(RuntimeException.class, () -> WithJava.run(executor, ForceProjector.class, "Obi Wan"));
            assertThat(throwable)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCauseExactlyInstanceOf(CloneNotSupportedException.class)
                .hasMessage("sandbox." + Jedi.class.getTypeName());
            return null;
        });
    }

    public static class Jedi {
        private final String name;

        Jedi(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public Jedi clone() {
            try {
                return (Jedi) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    public static class ForceProjector implements Function<String, String> {
        @Override
        public String apply(String subjectName) {
            Jedi jedi = new Jedi(subjectName);
            return jedi.clone().getName();
        }
    }
}
