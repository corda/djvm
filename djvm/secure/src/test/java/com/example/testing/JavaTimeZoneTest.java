package com.example.testing;

import net.corda.djvm.TypedTaskFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaTimeZoneTest extends TestBase {
    @Test
    void testAllZoneIDs() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String[] zoneIDs = WithJava.run(taskFactory, GetAllZoneIDs.class, null);
                assertThat(zoneIDs).hasSize(600);
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testDefaultZoneID() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String defaultZoneID = WithJava.run(taskFactory, GetDefaultZoneID.class, null);
                assertThat(defaultZoneID).isEqualTo("UTC");
            } catch (Exception e) {
                fail(e);
            }
        });
    }

    @Test
    void testDefaultTimeZone() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                String defaultTimeZone = WithJava.run(taskFactory, GetDefaultTimeZone.class, null);
                assertThat(defaultTimeZone).isEqualTo("Coordinated Universal Time");
            } catch (Exception e) {
                fail(e);
            }
        });
    }
}
