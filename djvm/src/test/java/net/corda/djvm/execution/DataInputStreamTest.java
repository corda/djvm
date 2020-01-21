package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class DataInputStreamTest extends TestBase {
    private static final String MESSAGE = "Hello World!";
    private static final double BIG_FRACTION = 97323.38238232d;
    private static final long BIG_NUMBER = 81738392L;
    private static final int NUMBER = 123456;

    DataInputStreamTest() {
        super(JAVA);
    }

    @Test
    void testReadingData() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeLong(BIG_NUMBER);
            dos.writeInt(NUMBER);
            dos.writeUTF(MESSAGE);
            dos.writeDouble(BIG_FRACTION);
        }
        InputStream input = new ByteArrayInputStream(baos.toByteArray());

        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object[] result = WithJava.run(taskFactory, DataStreamer.class, input);
                assertThat(result).isEqualTo(new Object[]{
                    BIG_NUMBER,
                    NUMBER,
                    MESSAGE,
                    BIG_FRACTION
                });
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class DataStreamer implements Function<InputStream, Object[]> {
        @Override
        public Object[] apply(InputStream input) {
            try (DataInputStream dis = new DataInputStream(input)) {
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
