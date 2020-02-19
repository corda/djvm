package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;

import javax.xml.bind.DatatypeConverter;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.condition.JRE.JAVA_8;

@EnabledOnJre(JAVA_8)
class DatatypeConverterTest extends TestBase {
    private static final byte[] BINARY = new byte[]{ 0x1f, 0x2e, 0x3d, 0x4c, 0x5b, 0x6a, 0x70 };
    private static final String TEXT = "1F2E3D4C5B6A70";

    DatatypeConverterTest() {
        super(JAVA);
    }

    @Test
    void testHexToBinary() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                byte[] result = WithJava.run(taskFactory, HexToBytes.class, TEXT);
                assertThat(result).isEqualTo(BINARY);
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class HexToBytes implements Function<String, byte[]> {
        @Override
        public byte[] apply(String input) {
            return DatatypeConverter.parseHexBinary(input);
        }
    }

    @Test
    void testBinaryToHex() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String result = WithJava.run(taskFactory, BytesToHex.class, BINARY);
                assertThat(result).isEqualTo(TEXT);
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class BytesToHex implements Function<byte[], String> {
        @Override
        public String apply(byte[] input) {
            // Corda apparently depends on this returning in
            // uppercase in order not to break hash values.
            return DatatypeConverter.printHexBinary(input);
        }
    }
}
