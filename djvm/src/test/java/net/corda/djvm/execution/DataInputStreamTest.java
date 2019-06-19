package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.function.Function;

import static net.corda.djvm.messages.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;

class DataInputStreamTest extends TestBase {
    private static final String MESSAGE = "Hello World!";
    private static final double BIG_FRACTION = 97323.38238232d;
    private static final long BIG_NUMBER = 81738392L;
    private static final int NUMBER = 123456;

    @Test
    void testReadingData() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeLong(BIG_NUMBER);
            dos.writeInt(NUMBER);
            dos.writeUTF(MESSAGE);
            dos.writeDouble(BIG_FRACTION);
        }
        byte[] input = baos.toByteArray();

        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<byte[], Object[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult success = WithJava.run(executor, DataStreamer.class, input);
            assertThat(success.getResult()).isEqualTo(new Object[]{
                BIG_NUMBER,
                NUMBER,
                MESSAGE,
                BIG_FRACTION
            });
            return null;
        });
    }

    public static class DataStreamer implements Function<byte[], Object[]> {
        @Override
        public Object[] apply(byte[] bytes) {
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            try (DataInputStream dis = new DataInputStream(bais)) {
                return new Object[] {
                    dis.readLong(),
                    dis.readInt(),
                    dis.readUTF(),
                    dis.readDouble()
                };
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
