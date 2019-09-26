package net.corda.djvm.execution;

import net.corda.djvm.DummyJar;
import net.corda.djvm.TestBase;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import static java.util.zip.Deflater.NO_COMPRESSION;
import static net.corda.djvm.DummyJar.*;
import static org.assertj.core.api.Assertions.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.corda.djvm.SandboxType.JAVA;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JarInputStreamTest extends TestBase {
    private static final int DATA_SIZE = 512;
    private static DummyJar testJar;

    JarInputStreamTest() {
        super(JAVA);
    }

    @BeforeAll
    static void setup(@TempDir Path testProjectDir) throws IOException {
        testJar = new DummyJar(testProjectDir, "jarstream").build((jar, path) -> {
            jar.setComment(JarInputStreamTest.class.getName());
            jar.setLevel(NO_COMPRESSION);

            // One directory entry (stored)
            putDirectoryOf(jar, JarInputStreamTest.class);

            // One compressed class file
            putCompressedClass(jar, JarInputStreamTest.class);

            // One compressed non-class file
            putCompressedEntry(jar, "binary.dat", arrayOfJunk(DATA_SIZE));

            // One uncompressed text file
            String text = "Jar: " + path.toAbsolutePath() + System.lineSeparator()
                + "Class: " + JarInputStreamTest.class.getName()
                + System.lineSeparator();
            putUncompressedEntry(jar, "comment.txt", text.getBytes(UTF_8));
        });
    }

    @Test
    void testReadingData() throws IOException {
        InputStream input;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Files.copy(testJar.getPath(), baos);
            input = new ByteArrayInputStream(baos.toByteArray());
        }
        sandbox(ctx -> {
            SandboxExecutor<InputStream, String[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<String[]> success = WithJava.run(executor, JarStreamer.class, input);
            assertNotNull(success.getResult());
            assertThat(success.getResult())
                .isEqualTo(new String[] {
                    "Manifest-Version: 1.0\r\n\r\n",
                    DummyJar.directoryOf(getClass()).getName(),
                    DummyJar.getResourceName(getClass()),
                    "binary.dat",
                    "comment.txt"
                });
            return null;
        });
    }

    public static class JarStreamer implements Function<InputStream, String[]> {
        @Override
        public String[] apply(InputStream input) {
            try (
                JarInputStream jar = new JarInputStream(input);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()
            ) {
                Manifest manifest = jar.getManifest();
                manifest.write(baos);

                List<String> entries = new LinkedList<>();
                entries.add(new String(baos.toByteArray(), UTF_8));

                JarEntry entry;
                while ((entry = jar.getNextJarEntry()) != null) {
                    entries.add(entry.getName());
                }
                return entries.toArray(new String[0]);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
