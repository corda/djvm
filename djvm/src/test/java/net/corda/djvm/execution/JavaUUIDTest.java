package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static net.corda.djvm.SandboxType.JAVA;
import static org.junit.jupiter.api.Assertions.*;

class JavaUUIDTest extends TestBase {
    JavaUUIDTest() {
        super(JAVA);
    }

    @Test
    void testUUID() {
        UUID uuid = UUID.randomUUID();
        parentedSandbox(ctx -> {
            try {
                Object sandboxUUID = parentClassLoader.createBasicInput().apply(uuid);
                assertEquals("sandbox.java.util.UUID", sandboxUUID.getClass().getName());
                assertEquals(uuid.toString(), sandboxUUID.toString());

                Object revert = parentClassLoader.createBasicOutput().apply(sandboxUUID);
                assertNotSame(uuid, revert);
                assertEquals(uuid, revert);
            } catch (Exception e) {
                fail(e);
            }
            return null;
        });
    }
}