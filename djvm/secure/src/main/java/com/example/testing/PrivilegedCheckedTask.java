package com.example.testing;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.function.Function;

public class PrivilegedCheckedTask implements Function<String, String> {
    @Override
    public String apply(String s) {
        // Try to elevate our privileges...
        return AccessController.doPrivileged(new DoCheckedAction());
    }
}

final class DoCheckedAction implements PrivilegedAction<String> {
    @Override
    public String run() {
        try {
            ((AutoCloseable) DoCheckedAction.class.getClassLoader()).close();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return "FAIL";
    }
}
