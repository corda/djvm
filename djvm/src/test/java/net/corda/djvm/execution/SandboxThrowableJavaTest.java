package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import net.corda.djvm.rules.RuleViolationError;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static net.corda.djvm.SandboxType.JAVA;
import static net.corda.djvm.Utilities.*;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SandboxThrowableJavaTest extends TestBase {
    SandboxThrowableJavaTest() {
        super(JAVA);
    }

    @Test
    void testUserExceptionHandling() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[] result = WithJava.run(taskFactory, ThrowAndCatchJavaExample.class, "Hello World!");
                assertThat(result)
                    .isEqualTo(new String[]{"FIRST FINALLY", "BASE EXCEPTION", "Hello World!", "SECOND FINALLY"});
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testCheckedExceptions() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                assertAll(
                    () -> {
                        String result = WithJava.run(taskFactory, JavaWithCheckedExceptions.class, "http://localhost:8080/hello/world");
                        assertThat(result).isEqualTo("/hello/world");
                    },
                    () -> {
                        String result = WithJava.run(taskFactory, JavaWithCheckedExceptions.class, "nasty string");
                        assertThat(result).isEqualTo("CATCH:Illegal character in path at index 5: nasty string");
                    }
                );
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testMultiCatchExceptions() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                assertAll(
                    () -> {
                        String result = WithJava.run(taskFactory, WithMultiCatchExceptions.class, 1);
                        assertThat(result).isEqualTo("sandbox.net.corda.djvm.execution.MyExampleException:1");
                    },
                    () -> {
                        String result = WithJava.run(taskFactory, WithMultiCatchExceptions.class, 2);
                        assertThat(result).isEqualTo("sandbox.net.corda.djvm.execution.MyOtherException:2");
                    },
                    () -> {
                        Throwable exception = assertThrows(RuntimeException.class,
                            () -> WithJava.run(taskFactory, WithMultiCatchExceptions.class, 3)
                        );
                        assertThat(exception)
                            .isExactlyInstanceOf(RuntimeException.class)
                            .hasMessage("sandbox.net.corda.djvm.execution.BigTroubleException -> 3")
                            .hasCauseExactlyInstanceOf(Exception.class);
                        assertThat(exception.getCause())
                            .hasMessage("sandbox.net.corda.djvm.execution.MyBaseException -> sandbox.net.corda.djvm.execution.BigTroubleException=3");
                    },
                    () -> {
                        Throwable exception = assertThrows(IllegalArgumentException.class,
                            () -> WithJava.run(taskFactory, WithMultiCatchExceptions.class, 4)
                        );
                        assertThat(exception)
                            .hasMessage("4")
                            .hasCauseExactlyInstanceOf(Exception.class);
                        assertThat(exception.getCause())
                            .hasMessage("sandbox.net.corda.djvm.execution.MyBaseException -> sandbox.java.lang.IllegalArgumentException=4");
                    },
                    () -> {
                        Throwable exception = assertThrows(UnsupportedOperationException.class,
                            () -> WithJava.run(taskFactory, WithMultiCatchExceptions.class, 1000)
                        );
                        assertThat(exception)
                            .hasMessage("Unknown")
                            .hasNoCause();
                    }
                );
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testMultiCatchWithDisallowedExceptions() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                assertAll(
                    () -> {
                        String result = WithJava.run(taskFactory, WithMultiCatchDisallowedExceptions.class, "Hello World!");
                        assertThat(result).isEqualTo("sandbox.net.corda.djvm.execution.MyExampleException:Hello World!");
                    },
                    () -> {
                        Throwable exception = assertThrows(RuleViolationError.class,
                            () -> WithJava.run(taskFactory, WithMultiCatchDisallowedExceptions.class, "")
                        );
                        assertThat(exception)
                            .hasMessage(CANNOT_CATCH)
                            .hasNoCause();
                    }
                );
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testSuppressedJvmExceptions() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Throwable exception = assertThrows(IllegalArgumentException.class,
                    () -> WithJava.run(taskFactory, WithSuppressedJvmExceptions.class, "Hello World!")
                );
                assertThat(exception)
                    .hasCauseExactlyInstanceOf(IOException.class)
                    .hasMessage("READ=Hello World!");
                assertThat(exception.getCause())
                    .hasMessage("READ=Hello World!");
                assertThat(exception.getCause().getSuppressed())
                    .hasSize(1)
                    .allMatch(t -> t instanceof IOException && t.getMessage().equals("CLOSING"));
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testSuppressedUserExceptions() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Throwable exception = assertThrows(IllegalArgumentException.class,
                    () -> WithJava.run(taskFactory, WithSuppressedUserExceptions.class, "Hello World!")
                );
                assertThat(exception)
                    .hasMessage("THROW: Hello World!")
                    .hasCauseExactlyInstanceOf(Exception.class);
                assertThat(exception.getCause())
                    .hasMessage("sandbox.net.corda.djvm.execution.MyExampleException -> THROW: Hello World!");
                assertThat(exception.getCause().getSuppressed())
                    .hasSize(1)
                    .allMatch(t -> t instanceof RuntimeException
                        && t.getMessage().equals("sandbox.net.corda.djvm.execution.BigTroubleException -> BadResource: Hello World!"));
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class ThrowAndCatchJavaExample implements Function<String, String[]> {
        @Override
        public String[] apply(String input) {
            List<String> data = new LinkedList<>();
            try {
                try {
                    throw new MyExampleException(input);
                } finally {
                    data.add("FIRST FINALLY");
                }
            } catch (MyBaseException e) {
                data.add("BASE EXCEPTION");
                data.add(e.getMessage());
            } catch (Exception e) {
                data.add("NOT THIS ONE!");
            } finally {
                data.add("SECOND FINALLY");
            }

            return data.toArray(new String[0]);
        }
    }

    public static class JavaWithCheckedExceptions implements Function<String, String> {
        @Override
        public String apply(String input) {
            try {
                return new URI(input).getPath();
            } catch (URISyntaxException e) {
                return "CATCH:" + e.getMessage();
            }
        }
    }

    public static class WithMultiCatchExceptions implements Function<Integer, String> {
        @Override
        public String apply(@NotNull Integer input) {
            try {
                switch(input) {
                    case 1: throw new MyExampleException("1");
                    case 2: throw new MyOtherException("2");
                    case 3: throw new BigTroubleException("3");
                    case 4: throw new IllegalArgumentException("4");
                    default: throw new UnsupportedOperationException("Unknown");
                }
            } catch (MyExampleException | MyOtherException e) {
                // Common exception type is MyBaseException
                return e.getClass().getName() + ':' + e.getMessage();
            } catch (BigTroubleException | IllegalArgumentException e) {
                // Common exception type is RuntimeException
                e.initCause(new MyBaseException(e.getClass().getName() + '=' + e.getMessage()));
                throw e;
            }
        }
    }

    public static class WithMultiCatchDisallowedExceptions implements Function<String, String> {
        @Override
        public String apply(@NotNull String input) {
            try {
                if (!input.isEmpty()) {
                    throw new MyExampleException(input);
                } else {
                    throwRuleViolationError();
                    return "FAIL";
                }
            } catch (MyExampleException | ThreadDeath e) {
                return e.getClass().getName() + ':' + e.getMessage();
            }
        }
    }

    public static class WithSuppressedJvmExceptions implements Function<String, String> {
        @Override
        public String apply(String input) {
            try (BadReader reader = new BadReader(input)) {
                throw new IOException("READ=" + reader.getName());
            } catch (IOException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
    }

    public interface MyResource extends AutoCloseable {
        String getName();
    }

    public static class BadReader implements MyResource {
        private final String name;

        BadReader(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void close() throws IOException {
            throw new IOException("CLOSING");
        }
    }

    public static class WithSuppressedUserExceptions implements Function<String, String> {
        @Override
        public String apply(String input) {
            try (MyResource resource = new BadResource(input)) {
                throw new MyExampleException("THROW: " + resource.getName());
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }
    }

    public static class BadResource implements MyResource {
        private final String name;

        BadResource(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void close() {
            throw new BigTroubleException("BadResource: " + getName());
        }
    }
}
