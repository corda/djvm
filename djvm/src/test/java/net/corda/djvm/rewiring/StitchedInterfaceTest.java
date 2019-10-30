package net.corda.djvm.rewiring;

import net.corda.djvm.TestBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class StitchedInterfaceTest extends TestBase {
    StitchedInterfaceTest() {
        super(JAVA);
    }

    @ParameterizedTest
    @CsvSource({
        "sandbox.java.lang.CharSequence,java.lang.CharSequence",
        "sandbox.java.lang.Iterable,java.lang.Iterable<T>",
        "sandbox.java.util.Iterator,java.util.Iterator<E>",
        "sandbox.java.lang.Comparable,java.lang.Comparable<T>",
        "sandbox.java.util.Comparator,java.util.Comparator<T>"
    })
    void testStitchedInterface(String sandboxName, String stitchedName) {
        sandbox(ctx -> {
            try {
                Class<?> sandboxClass = Class.forName(sandboxName, true, ctx.getClassLoader());
                List<String> generics = Arrays.stream(sandboxClass.getGenericInterfaces())
                    .map(Type::getTypeName)
                    .collect(toList());
                assertThat(generics).containsExactly(stitchedName);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }
}
