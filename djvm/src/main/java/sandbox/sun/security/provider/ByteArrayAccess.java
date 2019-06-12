package sandbox.sun.security.provider;

@SuppressWarnings({"WeakerAccess", "unused"})
final class ByteArrayAccess extends sandbox.java.lang.Object {

    // Convert Little Endian byte[] to int[].
    @SuppressWarnings("SameParameterValue")
    static void b2iLittle(byte[] source, int sourceOffset, int[] target, int targetOffset, int sourceSize) {
        if (sourceOffset < 0 || targetOffset < 0 || sourceOffset + sourceSize > source.length
                || target.length - targetOffset < (sourceSize / Integer.BYTES)) {
            throw new ArrayIndexOutOfBoundsException();
        }

        final int sourceEnd = sourceOffset + sourceSize;
        while (sourceOffset < sourceEnd) {
            target[targetOffset++] = (
                (source[++sourceOffset] & 0xFF) |
                (source[++sourceOffset] & 0xFF) << 8 |
                (source[++sourceOffset] & 0xFF) << 16 |
                (source[sourceOffset] & 0xFF) << 24
            );
            ++sourceOffset;
        }
    }

    static void b2iLittle64(byte[] source, int sourceOffset, int[] target) {
        b2iLittle(source, sourceOffset, target, 0, 64);
    }

    // Convert int[] to Little Endian byte[].
    static void i2bLittle(int[] source, int sourceOffset, byte[] target, int targetOffset, int targetSize) {
        if (sourceOffset < 0 || targetOffset < 0 || targetOffset + targetSize > target.length
                || source.length - sourceOffset < (targetSize / Integer.BYTES)) {
            throw new ArrayIndexOutOfBoundsException();
        }

        final int targetEnd = targetOffset + targetSize;
        while (targetOffset < targetEnd) {
            int value = source[sourceOffset++];
            target[targetOffset] = (byte) value;
            target[++targetOffset] = (byte) (value >>> 8);
            target[++targetOffset] = (byte) (value >>> 16);
            target[++targetOffset] = (byte) (value >>> 24);
            ++targetOffset;
        }
    }

    // Convert int to Little Endian byte[targetOffset..targetOffset+3].
    static void i2bLittle4(int source, byte[] target, int targetOffset) {
        if (targetOffset < 0 || targetOffset + Integer.BYTES > target.length) {
            throw new ArrayIndexOutOfBoundsException();
        }

        target[targetOffset] = (byte) source;
        target[++targetOffset] = (byte) (source >>> 8);
        target[++targetOffset] = (byte) (source >>> 16);
        target[++targetOffset] = (byte) (source >>> 24);
    }

    // Convert Big Endian byte[] to int[].
    @SuppressWarnings("SameParameterValue")
    static void b2iBig(byte[] source, int sourceOffset, int[] target, int targetOffset, int sourceSize) {
        if (sourceOffset < 0 || targetOffset < 0 || sourceOffset + sourceSize > source.length
              || target.length - targetOffset < (sourceSize / Integer.BYTES)) {
            throw new ArrayIndexOutOfBoundsException();
        }

        final int sourceEnd = sourceOffset + sourceSize;
        while (sourceOffset < sourceEnd) {
            target[targetOffset++] = (
                (source[sourceOffset] & 0xFF) << 24 |
                (source[++sourceOffset] & 0xFF) << 16 |
                (source[++sourceOffset] & 0xFF) << 8 |
                (source[++sourceOffset] & 0xFF)
            );
            ++sourceOffset;
        }
    }

    static void b2iBig64(byte[] source, int sourceOffset, int[] target) {
        b2iBig(source, sourceOffset, target, 0, 64);
    }

    // Convert int[] to Big Endian byte[].
    static void i2bBig(int[] source, int sourceOffset, byte[] target, int targetOffset, int targetSize) {
        if (sourceOffset < 0 || targetOffset < 0 || targetOffset + targetSize > target.length
                || source.length - sourceOffset < (targetSize / Integer.BYTES)) {
            throw new ArrayIndexOutOfBoundsException();
        }

        final int targetEnd = targetOffset + targetSize;
        while (targetOffset < targetEnd) {
            int value = source[sourceOffset++];
            target[targetOffset] = (byte) (value >>> 24);
            target[++targetOffset] = (byte) (value >>> 16);
            target[++targetOffset] = (byte) (value >>> 8);
            target[++targetOffset] = (byte) value;
            ++targetOffset;
        }
    }

    // Convert int to Big Endian byte[targetOffset..targetOffset+3].
    static void i2bBig4(int source, byte[] target, int targetOffset) {
        if (targetOffset < 0 || targetOffset + Integer.BYTES > target.length) {
            throw new ArrayIndexOutOfBoundsException();
        }

        target[targetOffset] = (byte) (source >>> 24);
        target[++targetOffset] = (byte) (source >>> 16);
        target[++targetOffset] = (byte) (source >>> 8);
        target[++targetOffset] = (byte) source;
    }

    // Convert Big Endian byte[] to long[].
    @SuppressWarnings("SameParameterValue")
    static void b2lBig(byte[] source, int sourceOffset, long[] target, int targetOffset, int sourceSize) {
        if (sourceOffset < 0 || targetOffset < 0 || sourceOffset + sourceSize > source.length
                || target.length - targetOffset < (sourceSize / Long.BYTES)) {
            throw new ArrayIndexOutOfBoundsException();
        }

        final int sourceEnd = sourceOffset + sourceSize;
        while (sourceOffset < sourceEnd) {
            target[targetOffset++] = (
                (long)(source[sourceOffset] & 0xFF) << 56 |
                (long)(source[++sourceOffset] & 0xFF) << 48 |
                (long)(source[++sourceOffset] & 0xFF) << 40 |
                (long)(source[++sourceOffset] & 0xFF) << 32 |
                (long)(source[++sourceOffset] & 0xFF) << 24 |
                (long)(source[++sourceOffset] & 0xFF) << 16 |
                (long)(source[++sourceOffset] & 0xFF) << 8 |
                (long)(source[++sourceOffset] & 0xFF)
            );
            ++sourceOffset;
        }
    }

    static void b2lBig128(byte[] source, int sourceOffset, long[] target) {
        b2lBig(source, sourceOffset, target, 0, 128);
    }

    // Convert Big Endian byte[] to long[].
    static void l2bBig(long[] source, int sourceOffset, byte[] target, int targetOffset, int targetSize) {
        if (sourceOffset < 0 || targetOffset < 0 || targetOffset + targetSize > target.length
                || source.length - sourceOffset < (targetSize / Long.BYTES)) {
            throw new ArrayIndexOutOfBoundsException();
        }

        final int targetEnd = targetOffset + targetSize;
        while (targetOffset < targetEnd) {
            long value = source[sourceOffset++];
            target[targetOffset] = (byte) (value >>> 56);
            target[++targetOffset] = (byte) (value >>> 48);
            target[++targetOffset] = (byte) (value >>> 40);
            target[++targetOffset] = (byte) (value >>> 32);
            target[++targetOffset] = (byte) (value >>> 24);
            target[++targetOffset] = (byte) (value >>> 16);
            target[++targetOffset] = (byte) (value >>> 8);
            target[++targetOffset] = (byte) value;
            ++targetOffset;
        }
    }
}
