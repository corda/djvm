package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;

class BitSetTest extends TestBase {
    private static final byte[] BITS = new byte[] { 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08 };
    private static final int POSITION = 16;

    BitSetTest() {
        super(JAVA);
    }

    @Test
    void testCreatingBitSet() {
        BitSet bitset = BitSet.valueOf(BITS);

        sandbox(ctx -> {
            SandboxExecutor<byte[], int[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<int[]> success = WithJava.run(executor, CreateBitSet.class, BITS);
            assertThat(success.getResult())
                .isEqualTo(new int[] {
                    bitset.length(),
                    bitset.cardinality(),
                    bitset.size(),
                    bitset.nextClearBit(POSITION),
                    bitset.previousClearBit(POSITION),
                    bitset.nextSetBit(POSITION),
                    bitset.previousSetBit(POSITION)
                });
            return null;
        });
    }

    public static class CreateBitSet implements Function<byte[], int[]> {
        @Override
        public int[] apply(byte[] bytes) {
            BitSet bits = BitSet.valueOf(bytes);
            return new int[] {
                bits.length(),
                bits.cardinality(),
                bits.size(),
                bits.nextClearBit(POSITION),
                bits.previousClearBit(POSITION),
                bits.nextSetBit(POSITION),
                bits.previousSetBit(POSITION)
            };
        }
    }
}
