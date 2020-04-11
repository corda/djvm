package net.corda.djvm;

@SuppressWarnings("unused")
public enum Untrusted {
    BAD,
    NAUGHTY,
    EVIL;

    private long evilTime;

    private Untrusted() {
        evilTime = System.currentTimeMillis();
    }

    long getEvilTime() {
        return evilTime;
    }
}
