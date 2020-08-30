package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.rewiring.SandboxClassLoader;
import net.corda.djvm.rules.RuleViolationError;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ImportTaskJavaTest extends TestBase {
    private static final String MESSAGE = "Hello Outside World!";

    ImportTaskJavaTest() {
        super(JAVA);
    }

    @Test
    void testImportTask() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Function<? super String, ?> importTask = classLoader.createForImport(
                    new DoMagic().andThen(classLoader.createBasicInput())
                );
                Object result = importTask.apply(MESSAGE);

                assertEquals(new DoMagic().apply(MESSAGE), result.toString());
                assertEquals("sandbox.java.lang.String", result.getClass().getName());
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class DoMagic implements Function<String, String> {
        @Override
        public String apply(String input) {
            return String.format(">>> %s <<<", input);
        }
    }

    @Test
    void testImportFailingTask() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Function<? super String, ?> importTask = classLoader.createForImport(
                    new Failing().andThen(classLoader.createBasicInput())
                );
                Throwable ex = assertThrows(RuntimeException.class, () -> importTask.apply(MESSAGE));
                assertThat(ex)
                    .isExactlyInstanceOf(RuntimeException.class)
                    .hasMessage("java.lang.IllegalArgumentException -> " + MESSAGE);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class Failing implements Function<String, String> {
        @Override
        public String apply(String input) {
            throw new IllegalArgumentException(input);
        }
    }

    @Test
    void testImportTaskWithStackOverflow() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Function<? super String, ?> importTask = classLoader.createForImport(
                    new StackOverflow().andThen(classLoader.createBasicInput())
                );
                Throwable ex = assertThrows(StackOverflowError.class, () -> importTask.apply(MESSAGE));
                assertThat(ex).isExactlyInstanceOf(StackOverflowError.class);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class StackOverflow implements Function<String, String> {
        @Override
        public String apply(String input) {
            return new StackOverflow().apply(input);
        }
    }

    @Test
    void testImportTaskWithError() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Function<? super String, ?> importTask = classLoader.createForImport(
                    new HasError().andThen(classLoader.createBasicInput())
                );
                Throwable ex = assertThrows(RuleViolationError.class, () -> importTask.apply(MESSAGE));
                assertThat(ex)
                    .isExactlyInstanceOf(RuleViolationError.class)
                    .hasMessage("java.lang.ExceptionInInitializerError -> I am broken!");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class HasError implements Function<String, String> {
        @Override
        public String apply(String input) {
            throw new ExceptionInInitializerError("I am broken!");
        }
    }
}
