package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Throwable error = assertThrows(NoSuchMethodError.class, () -> WithJava.run(taskFactory, StrictRandom.class, 0));
                assertThat(error)
                    .isExactlyInstanceOf(NoSuchMethodError.class)
                    .hasMessageContaining("sandbox.java.lang.StrictMath.random()")
                    .hasMessageFindingMatch("(double sandbox\\.|\\.random\\(\\)D)+");
            } catch(Exception e) {
                fail(e);
            }
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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Double[] result = WithJava.run(taskFactory, StrictTrigonometry.class, 0);
                assertThat(result).isEqualTo(new Double[]{
                    0.0,
                    -1.0,
                    0.0,
                    StrictMath.PI / 2.0,
                    StrictMath.PI / 2.0,
                    0.0,
                    StrictMath.PI / 4.0
                });
            } catch(Exception e) {
                fail(e);
            }
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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Double[] result = WithJava.run(taskFactory, StrictRoots.class, 64.0);
                assertThat(result)
                    .isEqualTo(new Double[]{8.0, 4.0, 13.0});
            } catch(Exception e) {
                fail(e);
            }
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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object[] result = WithJava.run(taskFactory, StrictMaxMin.class, 100);
                assertThat(result)
                    .isEqualTo(new Object[]{100.0d, 0.0d, 100.0f, 0.0f, 100L, 0L, 100, 0});
            } catch(Exception e) {
                fail(e);
            }
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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object[] result = WithJava.run(taskFactory, StrictAbsolute.class, -100);
                assertThat(result)
                    .isEqualTo(new Object[]{100.0d, 100.0f, 100L, 100});
            } catch(Exception e) {
                fail(e);
            }
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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object[] result = WithJava.run(taskFactory, StrictRound.class, inputs);
                assertThat(result)
                    .isEqualTo(new Object[]{2019, 2019L, 2021, 2021L});
            } catch(Exception e) {
                fail(e);
            }
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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Double[] result = WithJava.run(taskFactory, StrictExponents.class, 0);
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
            } catch(Exception e) {
                fail(e);
            }
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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Double[] result = WithJava.run(taskFactory, StrictAngles.class, 0);
                assertThat(result)
                    .isEqualTo(new Object[]{180.0, StrictMath.PI});
            } catch(Exception e) {
                fail(e);
            }
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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Double[] result = WithJava.run(taskFactory, StrictHyperbolics.class, 0.0);
                assertThat(result)
                    .isEqualTo(new Double[]{0.0, 1.0, 0.0});
            } catch(Exception e) {
                fail(e);
            }
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
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                assertAll(
                    () -> assertThat(WithJava.run(taskFactory, StrictRemainder.class, 10.0)).isEqualTo(3.0),
                    () -> assertThat(WithJava.run(taskFactory, StrictRemainder.class, 7.0)).isEqualTo(0.0),
                    () -> assertThat(WithJava.run(taskFactory, StrictRemainder.class, 5.0)).isEqualTo(-2.0)
                );
            } catch(Exception e) {
                fail(e);
            }
        });
    }

    public static class StrictRemainder implements Function<Double, Double> {
        @Override
        public Double apply(Double x) {
            return StrictMath.IEEEremainder(x, 7.0d);
        }
    }
}
