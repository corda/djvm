package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.rewiring.Predicates;
import net.corda.djvm.rewiring.SandboxClassLoader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.function.Function;
import java.util.function.Predicate;

import static net.corda.djvm.SandboxType.JAVA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class JavaSandboxPredicateTest extends TestBase {
    JavaSandboxPredicateTest() {
        super(JAVA);
    }

    @ParameterizedTest
    @EnumSource(ExampleEnum.class)
    void testPredicate(ExampleEnum example) {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Function<Class<? extends Predicate<?>>, ?> sandboxPredicate = Predicates.createSandboxPredicate(classLoader);
                Function<Class<? extends Predicate<?>>, ? extends Predicate<? super Object>> predicateFactory
                    = Predicates.createRawPredicateFactory(classLoader).compose(sandboxPredicate);
                Predicate<? super Object> isSandboxEnum = predicateFactory.apply(CheckEnum.class);
                Object sandboxEnum = classLoader.createBasicInput().apply(example);

                assertNotNull(sandboxEnum, "sandboxed enum should not be null");
                assertTrue(isSandboxEnum.test(sandboxEnum.getClass()), sandboxEnum + " should be sandbox.Enum");
                assertFalse(isSandboxEnum.test(example.getClass()), example + " should not be sandbox.Enum");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    public static class CheckEnum implements Predicate<Class<?>> {
        @Override
        public boolean test(@NotNull Class<?> clazz) {
            return clazz.isEnum();
        }
    }
}
