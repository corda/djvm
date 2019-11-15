package net.corda.djvm.execution;

import greymalkin.PureEvil;
import net.corda.djvm.TestBase;
import net.corda.djvm.WithJava;
import net.corda.djvm.rewiring.SandboxClassLoader;
import net.corda.djvm.rules.RuleViolationError;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static net.corda.djvm.Utilities.*;
import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MaliciousClassLoaderTest extends TestBase {
    MaliciousClassLoaderTest() {
        super(JAVA);
    }

    @Test
    void testWithAnEvilClassLoader() {
        sandbox(ctx -> {
            SandboxExecutor<String, String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            Throwable ex = assertThrows(NoSuchMethodError.class, () -> WithJava.run(executor, ActOfEvil.class, PureEvil.class.getName()));
            assertThat(ex)
                .hasMessageContaining("sandbox.java.lang.System.currentTimeMillis()")
                .hasMessageFindingMatch("(long sandbox\\.|\\.currentTimeMillis\\(\\)J)+")
                .hasNoCause();
            return null;
        });
    }

    public static class ActOfEvil implements Function<String, String> {
        @Override
        public String apply(String className) {
            ClassLoader evilLoader = new ClassLoader() {
                @Override
                public Class<?> loadClass(String className, boolean resolve) {
                    throwRuleViolationError();
                    return null;
                }

                @Override
                protected Class<?> findClass(String className) {
                    throwRuleViolationError();
                    return null;
                }
            };
            try {
                Class<?> evilClass = Class.forName(className, true, evilLoader);
                return evilClass.newInstance().toString();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testWithEvilParentClassLoader() {
        sandbox(ctx -> {
            SandboxExecutor<String, String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            Throwable ex = assertThrows(RuleViolationError.class, () -> WithJava.run(executor, ActOfEvilParent.class, PureEvil.class.getName()));
            assertThat(ex)
                .hasMessage("Disallowed reference to API; java.lang.ClassLoader(ClassLoader)")
                .hasNoCause();
            return null;
        });
    }

    public static class ActOfEvilParent implements Function<String, String> {
        @Override
        public String apply(String className) {
            ClassLoader evilLoader = new ClassLoader(null) {
                @Override
                public Class<?> loadClass(String className, boolean resolve) {
                    throwRuleViolationError();
                    return null;
                }

                @Override
                protected Class<?> findClass(String className) {
                    throwRuleViolationError();
                    return null;
                }
            };
            try {
                Class<?> evilClass = Class.forName(className, true, evilLoader);
                return evilClass.newInstance().toString();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }
    }

    @Test
    void testAccessingParentClassLoader() {
        sandbox(ctx -> {
            try {
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();
                Function<String, ClassLoader> getParentClassLoader = typedTaskFor(ctx.getClassLoader(), taskFactory, GetParentClassLoader.class);
                ClassLoader result = getParentClassLoader.apply("");
                assertThat(result)
                    .isExactlyInstanceOf(SandboxClassLoader.class)
                    .extracting(ClassLoader::getParent)
                    .isExactlyInstanceOf(SandboxClassLoader.class)
                    .isEqualTo(ctx.getClassLoader().getParent());
            } catch(Exception e) {
                fail(e);
            }
            return null;
        });
    }

    public static class GetParentClassLoader implements Function<String, ClassLoader> {
        @Override
        public ClassLoader apply(String input) {
            ClassLoader parent = ClassLoader.getSystemClassLoader();

            // In theory, this will iterate up the ClassLoader chain
            // until it locates the DJVM's application ClassLoader.
            while (parent.getClass().getClassLoader() != null && parent.getParent() != null) {
                parent = parent.getParent();
            }
            return parent;
        }
    }

    @Test
    void testClassLoaderForWhitelistedClass() {
        sandbox(ctx -> {
            SandboxExecutor<String, ClassLoader> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ClassLoader result = WithJava.run(executor, GetWhitelistedClassLoader.class, "").getResult();
            assertThat(result)
                .isExactlyInstanceOf(SandboxClassLoader.class);
            return null;
        });
    }

    public static class GetWhitelistedClassLoader implements Function<String, ClassLoader> {
        @Override
        public ClassLoader apply(String input) {
            // A whitelisted class belongs to the application classloader.
            return ClassLoader.class.getClassLoader();
        }
    }
}
