package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JavaURLTest extends TestBase {
    private static final int BUFFER_SIZE = 1024;
    private static final int EOF = -1;

    JavaURLTest() {
        super(JAVA);
    }

    @Test
    void testMarshallingURL() {
        URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
        parentedSandbox(ctx -> {
            try {
                Object sandboxURL = parentClassLoader.createBasicInput().apply(url);
                assertEquals("sandbox.java.net.URL", sandboxURL.getClass().getName());
                assertEquals(url.toString(), sandboxURL.toString());

                Object revert = parentClassLoader.createBasicOutput().apply(sandboxURL);
                assertSame(url, revert);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }

    @Test
    void testCreatingInvalidURL() {
        parentedSandbox(ctx -> {
            SandboxExecutor<String, URL> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            Throwable ex = assertThrows(RuntimeException.class,() -> WithJava.run(executor, CreateURL.class, "evil://muhaha!!!!"));
            assertThat(ex)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessageContaining("sandbox.java.net.MalformedURLException: unknown protocol: evil")
                .hasCauseExactlyInstanceOf(Exception.class);
            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://localhost:8080",
        "https://localhost:8080",
        "file://data.txt",
        "file://rt.jar!/META-INF/MANIFEST.MF"
    })
    void testForbiddenURL(String url) {
        String protocol = url.substring(0, url.indexOf(':'));
        parentedSandbox(ctx -> {
            SandboxExecutor<String, URL> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            Throwable ex = assertThrows(RuntimeException.class,() -> WithJava.run(executor, CreateURL.class, url));
            assertThat(ex)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessageContaining("sandbox.java.net.MalformedURLException: Forbidden protocol: " + protocol)
                .hasCauseExactlyInstanceOf(Exception.class);
            return null;
        });
    }

    public static class CreateURL implements Function<String, URL> {
        @Override
        public URL apply(String text) {
            try {
                return new URL(text);
            } catch (MalformedURLException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Test
    void testOpeningConnection() {
        URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
        byte[] contents = new GetURLData().apply(url);
        assertThat(contents).isNotEmpty();

        parentedSandbox(ctx -> {
            SandboxExecutor<URL, byte[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<byte[]> output = WithJava.run(executor, GetURLData.class, url);
            assertThat(output.getResult()).isEqualTo(contents);
            return null;
        });
    }

    public static class GetURLData implements Function<URL, byte[]> {
        @Override
        public byte[] apply(URL url) {
            try (
                InputStream input = url.openStream();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
            ){
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = input.read(buffer)) != EOF) {
                    output.write(buffer, 0, bytesRead);
                }

                return output.toByteArray();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}