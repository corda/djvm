package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
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
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = ctx.getClassLoader().createTaskFactory();

                Function<? super String, String[]> chronologyTask = typedTaskFor(ctx.getClassLoader(), taskFactory, GetChronologyNames.class);
                String[] chronologies = chronologyTask.apply(null);
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
            return null;
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
