package sandbox.java.util.zip;

import sandbox.java.lang.DJVM;

@SuppressWarnings("unused")
public class Inflater extends sandbox.java.lang.Object {

    private final java.util.zip.Inflater inflater;

    public Inflater(boolean nowrap) {
        inflater = new java.util.zip.Inflater(nowrap);
    }

    public Inflater() {
        inflater = new java.util.zip.Inflater();
    }

    public void setInput(byte[] buffer, int offset, int length) {
        inflater.setInput(buffer, offset, length);
    }

    public void setInput(byte[] buffer) {
        inflater.setInput(buffer);
    }

    public void setDictionary(byte[] buffer, int offset, int length) {
        inflater.setDictionary(buffer, offset, length);
    }

    public void setDictionary(byte[] buffer) {
        inflater.setDictionary(buffer);
    }

    public int getRemaining() {
        return inflater.getRemaining();
    }

    public boolean needsInput() {
        return inflater.needsInput();
    }

    public boolean needsDictionary() {
        return inflater.needsDictionary();
    }

    public boolean finished() {
        return inflater.finished();
    }

    // This function is really throwing the sandbox's "special"
    // equivalent of Java's checked DataFormatException.
    public int inflate(byte[] buffer, int offset, int length) throws Exception {
        try {
            return inflater.inflate(buffer, offset, length);
        } catch (java.util.zip.DataFormatException e) {
            throw (Exception) DJVM.fromDJVM(DJVM.doCatch(e));
        }
    }

    // This function is really throwing the sandbox's "special"
    // equivalent of Java's checked DataFormatException.
    public int inflate(byte[] buffer) throws Exception {
        try {
            return inflater.inflate(buffer);
        } catch (java.util.zip.DataFormatException e) {
            throw (Exception) DJVM.fromDJVM(DJVM.doCatch(e));
        }
    }

    public int getAdler() {
        return inflater.getAdler();
    }

    public int getTotalIn() {
        return inflater.getTotalIn();
    }

    public long getBytesRead() {
        return inflater.getBytesRead();
    }

    public int getTotalOut() {
        return inflater.getTotalOut();
    }

    public long getBytesWritten() {
        return inflater.getBytesWritten();
    }

    public void reset() {
        inflater.reset();
    }

    public void end() {
        inflater.end();
    }
}
