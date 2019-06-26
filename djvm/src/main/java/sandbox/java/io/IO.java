package sandbox.java.io;

import java.io.IOException;

public final class IO {
    public static InputStream toDJVM(java.io.InputStream input) {
        return input == null ? null : new DJVMInputStream(input);
    }

    /**
     * Create an instance of {@link InputStream} by wrapping {@link java.io.InputStream}.
     */
    private static class DJVMInputStream extends InputStream {
        private final java.io.InputStream input;

        DJVMInputStream(java.io.InputStream input) {
            this.input = input;
        }

        @Override
        public void close() throws IOException {
            input.close();
        }

        @Override
        public int read() throws IOException {
            return input.read();
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            return input.read(buffer, offset, length);
        }

        @Override
        public long skip(long length) throws IOException {
            return input.skip(length);
        }

        @Override
        public int available() throws IOException {
            return input.available();
        }

        @Override
        public boolean markSupported() {
            return input.markSupported();
        }

        @Override
        public void mark(int readLimit) {
            input.mark(readLimit);
        }

        @Override
        public void reset() throws IOException {
            input.reset();
        }
    }
}
