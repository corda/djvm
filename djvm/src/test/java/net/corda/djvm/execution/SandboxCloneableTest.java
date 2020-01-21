package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SandboxCloneableTest extends TestBase {
    SandboxCloneableTest() {
        super(JAVA);
    }

    @Test
    void testCloningInsideSandbox() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String result = WithJava.run(taskFactory, CloningMachine.class, "Jango Fett");
                assertThat(result).isEqualTo("Jango Fett");
            } catch(Exception e) {
                fail(e);
            }
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
        public Soldier clone() throws CloneNotSupportedException {
            return (Soldier) super.clone();
        }
    }

    public static class CloningMachine implements Function<String, String> {
        @Override
        public String apply(String subjectName) {
            Soldier soldier = new Soldier(subjectName);
            try {
                return soldier.clone().getName();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testFailedCloningInsideSandbox() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Throwable throwable = assertThrows(RuntimeException.class, () -> WithJava.run(taskFactory, ForceProjector.class, "Obi Wan"));
                assertThat(throwable)
                    .isExactlyInstanceOf(RuntimeException.class)
                    .hasCauseExactlyInstanceOf(CloneNotSupportedException.class)
                    .hasMessage("sandbox." + Jedi.class.getTypeName());
            } catch(Exception e) {
                fail(e);
            }
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
        public Jedi clone() throws CloneNotSupportedException {
            return (Jedi) super.clone();
        }
    }

    public static class ForceProjector implements Function<String, String> {
        @Override
        public String apply(String subjectName) {
            Jedi jedi = new Jedi(subjectName);
            try {
                return jedi.clone().getName();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }
}
