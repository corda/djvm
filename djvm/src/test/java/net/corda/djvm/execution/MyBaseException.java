package net.corda.djvm.execution;

@SuppressWarnings("WeakerAccess")
public class MyBaseException extends Exception {
    public MyBaseException(String message) {
        super(message);
    }
}
