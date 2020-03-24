package com.example.testing;

import net.corda.djvm.TypedTaskFactory;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaTimeZoneTest extends TestBase {
    @Test
    void testAllZoneIDs() {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Function<?, String[]> allZoneIDs = taskFactory.create(GetAllZoneIDs.class);
                String[] zoneIDs = allZoneIDs.apply(null);
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
                Function<?, String> defaultZoneIdTask = taskFactory.create(GetDefaultZoneID.class);
                String defaultZoneID = defaultZoneIdTask.apply(null);
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
                Function<?, String> defaultTimeZoneTask = taskFactory.create(GetDefaultTimeZone.class);
                String defaultTimeZone = defaultTimeZoneTask.apply(null);
                assertThat(defaultTimeZone).isEqualTo("Coordinated Universal Time");
            } catch (Exception e) {
                fail(e);
            }
        });
    }
}
