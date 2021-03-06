package com.example.osgi.testing;

import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaChronologyTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(JavaChronologyTest.class);
    private static final String GET_CHRONOLOGY_NAMES = "com.example.testing.GetChronologyNames";

    @Test
    void testChronologyNames() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                String[] chronologies = WithJava.run(classLoader, GET_CHRONOLOGY_NAMES, null);
                assertThat(chronologies).contains(
                    "Hijrah-umalqura",
                    "ISO",
                    "Japanese",
                    "Minguo",
                    "ThaiBuddhist"
                );
            } catch (Exception e) {
                LOG.error("Failed", e);
                fail(e);
            }
        });
    }
}
