package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static net.corda.djvm.messages.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.*;

class SandboxStrictMathTest extends TestBase {
    private static final double ERROR_DELTA = 1.0E-10;

    SandboxStrictMathTest() {
        super(JAVA);
    }

    @Test
    void testStrictMathHasNoRandom() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<Integer, Double> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            Throwable error = assertThrows(NoSuchMethodError.class, () -> WithJava.run(executor, StrictRandom.class, 0));
            assertThat(error)
                .isExactlyInstanceOf(NoSuchMethodError.class)
                .hasMessage("sandbox.java.lang.StrictMath.random()D");
            return null;
        });
    }

    public static class StrictRandom implements Function<Integer, Double> {
        @Override
        public Double apply(Integer seed) {
            return StrictMath.random();
        }
    }

    @Test
    void testStrictMathHasTrigonometry() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<Integer, Double[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<Double[]> success = WithJava.run(executor, StrictTrigonometry.class, 0);
            assertThat(success.getResult()).isEqualTo(new Double[] {
                0.0,
                -1.0,
                0.0,
                StrictMath.PI / 2.0,
                StrictMath.PI / 2.0,
                0.0,
                StrictMath.PI / 4.0
            });
            return null;
        });
    }

    public static class StrictTrigonometry implements Function<Integer, Double[]> {
        @Override
        public Double[] apply(Integer input) {
            return new Double[] {
                StrictMath.floor(StrictMath.sin(StrictMath.PI)),
                StrictMath.cos(StrictMath.PI),
                StrictMath.tan(0.0),
                StrictMath.acos(0.0),
                StrictMath.asin(1.0),
                StrictMath.atan(0.0),
                StrictMath.atan2(1.0, 1.0)
            };
        }
    }

    @Test
    void testStrictMathRoots() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<Double, Double[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<Double[]> success = WithJava.run(executor, StrictRoots.class, 64.0);
            assertThat(success.getResult())
                .isEqualTo(new Double[] { 8.0, 4.0, 13.0 });
            return null;
        });
    }

    public static class StrictRoots implements Function<Double, Double[]> {
        @Override
        public Double[] apply(Double input) {
            return new Double[] {
                StrictMath.sqrt(input),
                StrictMath.cbrt(input),
                StrictMath.hypot(5.0, 12.0)
            };
        }
    }

    @Test
    void testStrictMaxMin() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<Integer, Object[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<Object[]> success = WithJava.run(executor, StrictMaxMin.class, 100);
            assertThat(success.getResult())
                .isEqualTo(new Object[] { 100.0d, 0.0d, 100.0f, 0.0f, 100L, 0L, 100, 0 });
            return null;
        });
    }

    public static class StrictMaxMin implements Function<Integer, Object[]> {
        @Override
        public Object[] apply(Integer input) {
            return new Object[] {
                StrictMath.max((double) input, 0.0d),
                StrictMath.min((double) input, 0.0d),
                StrictMath.max((float) input, 0.0f),
                StrictMath.min((float) input, 0.0f),
                StrictMath.max((long) input, 0L),
                StrictMath.min((long) input, 0L),
                StrictMath.max(input, 0),
                StrictMath.min(input, 0)
            };
        }
    }

    @Test
    void testStrictAbsolute() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<Integer, Object[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<Object[]> success = WithJava.run(executor, StrictAbsolute.class, -100);
            assertThat(success.getResult())
                .isEqualTo(new Object[] { 100.0d, 100.0f, 100L, 100 });
            return null;
        });
    }

    public static class StrictAbsolute implements Function<Integer, Object[]> {
        @Override
        public Object[] apply(Integer input) {
            return new Object[] {
                StrictMath.abs((double) input),
                StrictMath.abs((float) input),
                StrictMath.abs((long) input),
                StrictMath.abs(input)
            };
        }
    }

    @Test
    void testStrictRound() {
        Double[] inputs = new Double[] { 2019.3, 2020.9 };
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<Double[], Object[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<Object[]> success = WithJava.run(executor, StrictRound.class, inputs);
            assertThat(success.getResult())
                .isEqualTo(new Object[]{ 2019, 2019L, 2021, 2021L });
            return null;
        });
    }

    public static class StrictRound implements Function<Double[], Object[]> {
        @Override
        public Object[] apply(Double[] inputs) {
            List<Object> results = new ArrayList<>();
            for (Double input : inputs) {
                results.add(StrictMath.round(input.floatValue()));
                results.add(StrictMath.round(input));
            }
            return results.toArray();
        }
    }

    @Test
    void testStrictExponents() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<Integer, Double[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            Double[] result = WithJava.run(executor, StrictExponents.class, 0).getResult();
            assertNotNull(result);
            assertAll(
                () -> assertThat(result).hasSize(6),
                () -> assertThat(result[0]).isEqualTo(81.0),
                () -> assertThat(result[1]).isEqualTo(1.0),
                () -> assertThat(result[2]).isEqualTo(3.0),
                () -> assertThat(result[3]).isEqualTo(StrictMath.E, within(ERROR_DELTA)),
                () -> assertThat(result[4]).isEqualTo(StrictMath.E - 1.0, within(ERROR_DELTA)),
                () -> assertThat(result[5]).isEqualTo(1.0, within(ERROR_DELTA))
            );
            return null;
        });
    }

    public static class StrictExponents implements Function<Integer, Double[]> {
        @Override
        public Double[] apply(Integer input) {
            return new Double[] {
                StrictMath.pow(3.0, 4.0),
                StrictMath.log(StrictMath.E),
                StrictMath.log10(1000.0),
                StrictMath.exp(1.0),
                StrictMath.expm1(1.0),
                StrictMath.log1p(StrictMath.E - 1.0)
            };
        }
    }

    @Test
    void testStrictAngles() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<Integer, Double[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<Double[]> success = WithJava.run(executor, StrictAngles.class, 0);
            assertThat(success.getResult())
                .isEqualTo(new Object[]{ 180.0, StrictMath.PI });
            return null;
        });
    }

    public static class StrictAngles implements Function<Integer, Double[]> {
        @Override
        public Double[] apply(Integer input) {
            return new Double[] {
                StrictMath.toDegrees(StrictMath.PI),
                StrictMath.toRadians(180.0)
            };
        }
    }

    @Test
    void testStrictHyperbolics() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<Double, Double[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<Double[]> success = WithJava.run(executor, StrictHyperbolics.class, 0.0);
            assertThat(success.getResult())
                .isEqualTo(new Double[]{ 0.0, 1.0, 0.0 });
            return null;
        });
    }

    public static class StrictHyperbolics implements Function<Double, Double[]> {
        @Override
        public Double[] apply(Double x) {
            return new Double[] {
                StrictMath.sinh(x),
                StrictMath.cosh(x),
                StrictMath.tanh(x)
            };
        }
    }

    @Test
    void testStrictRemainder() {
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<Double, Double> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            assertAll(
                () -> assertThat(WithJava.run(executor, StrictRemainder.class, 10.0).getResult()).isEqualTo(3.0),
                () -> assertThat(WithJava.run(executor, StrictRemainder.class, 7.0).getResult()).isEqualTo(0.0),
                () -> assertThat(WithJava.run(executor, StrictRemainder.class, 5.0).getResult()).isEqualTo(-2.0)
            );
            return null;
        });
    }

    public static class StrictRemainder implements Function<Double, Double> {
        @Override
        public Double apply(Double x) {
            return StrictMath.IEEEremainder(x, 7.0d);
        }
    }
}
