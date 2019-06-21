package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.WithJava;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import static net.corda.djvm.messages.Severity.WARNING;
import static org.assertj.core.api.Assertions.assertThat;

class SandboxConcurrentHashMapTest extends TestBase {
    @Test
    void testJoiningIterableInsideSandbox() {
        String[] inputs = new String[]{ "one", "One", "ONE" };
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<String[], String> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult success = WithJava.run(executor, CreateMap.class, inputs);
            assertThat(success.getResult()).isEqualTo("[one has 3]");
            return null;
        });
    }

    public static class CreateMap implements Function<String[], String> {
        private final ConcurrentMap<String, Data> map = new ConcurrentHashMap<>();

        @Override
        public String apply(@NotNull String[] strings) {
            for (String s : strings) {
                Data data = map.computeIfAbsent(s.toLowerCase(), k -> new Data(0));
                data.increment();
            }

            StringBuilder result = new StringBuilder();
            map.forEach((k, v) -> result.append('[').append(k).append(" has ").append(v).append(']'));
            return result.toString();
        }

        private static class Data {
            private int value;

            Data(int value) {
                this.value = value;
            }

            int getValue() {
                return value;
            }

            void increment() {
                ++value;
            }

            @Override
            public String toString() {
                return Integer.toString(getValue());
            }
        }
    }

    @Test
    void testStreamOfKeys() {
        Integer[] inputs = new Integer[]{ 1, 2, 3 };
        parentedSandbox(WARNING, true, ctx -> {
            SandboxExecutor<Integer[], Integer> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult success = WithJava.run(executor, KeyStreamMap.class, inputs);
            assertThat(success.getResult()).isEqualTo(6);
            return null;
        });
    }

    public static class KeyStreamMap implements Function<Integer[], Integer> {
        private final ConcurrentMap<Integer, String> map = new ConcurrentHashMap<>();

        @Override
        public Integer apply(@NotNull Integer[] input) {
            for (Integer i : input) {
                map.putIfAbsent(i, Integer.toString(i));
            }
            return map.keySet().stream()
                .mapToInt(Integer::intValue)
                .sum();
        }
    }
}
