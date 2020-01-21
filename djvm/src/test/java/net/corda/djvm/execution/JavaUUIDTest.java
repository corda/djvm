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
        sandbox(ctx -> {
            try {
                Object sandboxUUID = ctx.getClassLoader().createBasicInput().apply(uuid);
                assertEquals("sandbox.java.util.UUID", sandboxUUID.getClass().getName());
                assertEquals(uuid.toString(), sandboxUUID.toString());

                Object revert = ctx.getClassLoader().createBasicOutput().apply(sandboxUUID);
                assertNotSame(uuid, revert);
                assertEquals(uuid, revert);
            } catch (Exception e) {
                fail(e);
            }
        });
    }
}