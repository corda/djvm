package net.corda.djvm.execution;

import greymalkin.PureEvil;
import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
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
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                RuleViolationError ex = assertThrows(RuleViolationError.class,
                    () -> WithJava.run(taskFactory, ActOfEvil.class, PureEvil.class.getName())
                );
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.System.currentTimeMillis()")
                    .hasNoCause();
            } catch(Exception e){
                fail(e);
            }
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
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Throwable ex = assertThrows(RuleViolationError.class, () -> WithJava.run(taskFactory, ActOfEvilParent.class, PureEvil.class.getName()));
                assertThat(ex)
                    .hasMessage("Disallowed reference to API; java.lang.ClassLoader(ClassLoader)")
                    .hasNoCause();
            } catch(Exception e) {
                fail(e);
            }
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
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                ClassLoader result = WithJava.run(taskFactory, GetParentClassLoader.class, "");
                assertThat(result)
                    .isExactlyInstanceOf(SandboxClassLoader.class)
                    .extracting(ClassLoader::getParent)
                    .isExactlyInstanceOf(SandboxClassLoader.class)
                    .isEqualTo(ctx.getClassLoader().getParent());
            } catch(Exception e) {
                fail(e);
            }
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
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                ClassLoader result = WithJava.run(taskFactory, GetWhitelistedClassLoader.class, "");
                assertThat(result)
                    .isExactlyInstanceOf(SandboxClassLoader.class);
            } catch(Exception e) {
                fail(e);
            }
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
