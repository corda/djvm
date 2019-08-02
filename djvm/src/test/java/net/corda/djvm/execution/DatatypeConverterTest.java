package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import javax.xml.bind.DatatypeConverter;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static net.corda.djvm.messages.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;

class DatatypeConverterTest extends TestBase {
    private static final byte[] BINARY = new byte[]{ 0x1f, 0x2e, 0x3d, 0x4c, 0x5b, 0x6a, 0x70 };
    private static final String TEXT = "1F2E3D4C5B6A70";

    DatatypeConverterTest() {
        super(JAVA);
    }

    @Test
    void testHexToBinary() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<String, byte[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult success = WithJava.run(executor, HexToBytes.class, TEXT);
            assertThat(success.getResult()).isEqualTo(BINARY);
            return null;
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
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<byte[], String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult success = WithJava.run(executor, BytesToHex.class, BINARY);
            assertThat(success.getResult()).isEqualTo(TEXT);
            return null;
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
