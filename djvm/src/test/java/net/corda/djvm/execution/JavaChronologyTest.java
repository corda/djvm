package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.time.chrono.Chronology;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaChronologyTest extends TestBase {
    JavaChronologyTest() {
        super(JAVA);
    }

    @Test
    void testAvailableChronologies() {
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

    public static class GetChronologyNames implements Function<String, String[]> {
        @Override
        public String[] apply(String s) {
            return Chronology.getAvailableChronologies().stream()
                .map(Chronology::getId)
                .toArray(String[]::new);
        }
    }
}
