package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.fail;

class JavaMathTest extends TestBase {
    JavaMathTest() {
        super(JAVA);
    }

    @Test
    void testBigIntegerMarshalledFaithfully() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Function<? super Object, ?> basicInput = classLoader.createBasicInput();
                Function<? super Object, ?> basicOutput = classLoader.createBasicOutput();

                BigInteger originalValue = new BigInteger("12345678901234567890");
                Object sandboxValue = basicInput.apply(originalValue);
                assertEquals("sandbox.java.math.BigInteger", sandboxValue.getClass().getName());

                BigInteger returnedValue = (BigInteger) basicOutput.apply(sandboxValue);
                assertEquals(originalValue, returnedValue);
                assertNotSame(originalValue, returnedValue);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testBigDecimalMarshalledFaithfully() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Function<? super Object, ?> basicInput = classLoader.createBasicInput();
                Function<? super Object, ?> basicOutput = classLoader.createBasicOutput();

                BigDecimal originalValue = new BigDecimal("1234567890555.123456789");
                Object sandboxValue = basicInput.apply(originalValue);
                assertEquals("sandbox.java.math.BigDecimal", sandboxValue.getClass().getName());

                BigDecimal returnedValue = (BigDecimal) basicOutput.apply(sandboxValue);
                assertEquals(originalValue, returnedValue);
                assertNotSame(originalValue, returnedValue);
            } catch (Exception e) {
                fail(e);
            }
        });
    }
}
