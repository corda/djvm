package sandbox.java.util;

import sandbox.java.io.DataInputStream;
import sandbox.java.lang.DJVM;
import sandbox.java.lang.String;
import sandbox.java.security.AccessController;
import sandbox.java.security.PrivilegedAction;

import java.io.IOException;
import java.io.Serializable;

/**
 * This is a dummy class that implements just enough of {@link java.util.Currency}
 * to allow us to compile the anonymous inner class implementing {@link PrivilegedAction}.
 */
@SuppressWarnings({"unused", "RedundantThrows", "WeakerAccess"})
public final class Currency extends sandbox.java.lang.Object implements Serializable {
    private static final java.lang.String NOT_IMPLEMENTED = "Dummy class - not implemented";

    private static final int MAGIC_NUMBER = 0x43757244;
    private static final int A_TO_Z = ('Z' - 'A') + 1;
    private static final int VALID_FORMAT_VERSION = 2;

    static int formatVersion;
    static int dataVersion;
    static int[] mainTable;
    static long[] scCutOverTimes;
    static String[] scOldCurrencies;
    static String[] scNewCurrencies;
    static int[] scOldCurrenciesDFD;
    static int[] scNewCurrenciesDFD;
    static int[] scOldCurrenciesNumericCode;
    static int[] scNewCurrenciesNumericCode;
    static String otherCurrencies;
    static int[] otherCurrenciesDFD;
    static int[] otherCurrenciesNumericCode;

    static {
        /*
         * This anonymous inner class is a NON-dummy class called java.util.Currency$1.
         */
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                try (DataInputStream dis = DJVM.loadSystemResource("currency.data")) {
                    if (dis.readInt() != MAGIC_NUMBER) {
                        throw new InternalError("Currency data is possibly corrupted");
                    }
                    formatVersion = dis.readInt();
                    if (formatVersion != VALID_FORMAT_VERSION) {
                        throw new InternalError("Currency data format is incorrect");
                    }
                    dataVersion = dis.readInt();
                    mainTable = readIntArray(dis, A_TO_Z * A_TO_Z);
                    int specialCaseCount = dis.readInt();
                    scCutOverTimes = readLongArray(dis, specialCaseCount);
                    scOldCurrencies = readStringArray(dis, specialCaseCount);
                    scNewCurrencies = readStringArray(dis, specialCaseCount);
                    scOldCurrenciesDFD = readIntArray(dis, specialCaseCount);
                    scNewCurrenciesDFD = readIntArray(dis, specialCaseCount);
                    scOldCurrenciesNumericCode = readIntArray(dis, specialCaseCount);
                    scNewCurrenciesNumericCode = readIntArray(dis, specialCaseCount);
                    int otherCount = dis.readInt();
                    otherCurrencies = dis.readUTF();
                    otherCurrenciesDFD = readIntArray(dis, otherCount);
                    otherCurrenciesNumericCode = readIntArray(dis, otherCount);
                } catch (IOException e) {
                    throw new InternalError(e);
                }
                return null;
            }
        });
    }

    private static int[] readIntArray(DataInputStream dis, int count) throws IOException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    private static long[] readLongArray(DataInputStream dis, int count) throws IOException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    private static String[] readStringArray(DataInputStream dis, int count) throws IOException {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }
}
