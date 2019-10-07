package com.example.testing;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;

/**
 * The DJVM refuses even to try loading classes from the
 * net.corda.djvm package into the sandbox. And so this
 * class lives in a "safe" package "com.example.testing".
 */
public class HasBrokenInitializer {
    static {
        if (HasBrokenInitializer.class.getName().startsWith("sandbox.")) {
            throw new UncheckedIOException(new FileNotFoundException("missing.dat"));
        }
    }
}
