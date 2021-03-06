package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class Base64Test extends TestBase {
    private static final String MESSAGE = "Round and round the rugged rocks...";
    private static final String BASE64 = Base64.getEncoder().encodeToString(MESSAGE.getBytes(UTF_8));

    Base64Test() {
        super(JAVA);
    }

    @Test
    void testBase64ToBinary() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                byte[] result = WithJava.run(taskFactory, Base64ToBytes.class, BASE64);
                assertNotNull(result);
                assertThat(new String(result, UTF_8)).isEqualTo(MESSAGE);
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class Base64ToBytes implements Function<String, byte[]> {
        @Override
        public byte[] apply(String input) {
            return Base64.getDecoder().decode(input);
        }
    }

    @Test
    void testBinaryToBase64() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String result = WithJava.run(taskFactory, BytesToBase64.class, MESSAGE.getBytes(UTF_8));
                assertThat(result).isEqualTo(BASE64);
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class BytesToBase64 implements Function<byte[], String> {
        @Override
        public String apply(byte[] input) {
            return Base64.getEncoder().encodeToString(input);
        }
    }
}
