package net.corda.djvm;

import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;

@FunctionalInterface
public interface JarWriter {

    void write(JarOutputStream jar, Path path) throws IOException;

}