package com.example.osgi.testing;

import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaTimeZoneTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(JavaTimeZoneTest.class);

    @Test
    void testAllZoneIDs() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                String[] zoneIDs = WithJava.<String, String[]>run(classLoader, "com.example.testing.GetAllZoneIDs", null);
                assertThat(zoneIDs).hasSize(600);
            } catch (Exception e) {
                LOG.error("Failed", e);
                fail(e);
            }
        });
    }

    @Test
    void testDefaultZoneID() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                String defaultZoneID = WithJava.<String, String>run(classLoader, "com.example.testing.GetDefaultZoneID", null);
                assertThat(defaultZoneID).isEqualTo("UTC");
            } catch (Exception e) {
                LOG.error("Failed", e);
                fail(e);
            }
        });
    }

    @Test
    void testDefaultTimeZone() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                String defaultTimeZone = WithJava.<String, String>run(classLoader, "com.example.testing.GetDefaultTimeZone", null);
                assertThat(defaultTimeZone).isEqualTo("Coordinated Universal Time");
            } catch (Exception e) {
                LOG.error("Failed", e);
                fail(e);
            }
        });
    }
}
