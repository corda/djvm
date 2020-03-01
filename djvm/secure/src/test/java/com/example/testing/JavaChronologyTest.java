package com.example.testing;

import net.corda.djvm.TypedTaskFactory;
import org.junit.jupiter.api.Test;

import static com.example.testing.SandboxType.JAVA;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaChronologyTest extends TestBase {
    JavaChronologyTest() {
        super(JAVA);
    }

    @Test
    void testChronologyNames() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[] chronologies = WithJava.run(taskFactory, GetChronologyNames.class, null);
                assertThat(chronologies).contains(
                    "Hijrah-umalqura",
                    "ISO",
                    "Japanese",
                    "Minguo",
                    "ThaiBuddhist"
                );
            } catch (Exception e) {
                fail(e);
            }
        });
    }
}
