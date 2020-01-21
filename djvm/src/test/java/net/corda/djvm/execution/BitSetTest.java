package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.TypedTaskFactory;
import net.corda.djvm.WithJava;
import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

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
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                int[] result = WithJava.run(taskFactory, CreateBitSet.class, BITS);
                assertThat(result)
                    .isEqualTo(new int[]{
                        bitset.length(),
                        bitset.cardinality(),
                        bitset.size(),
                        bitset.nextClearBit(POSITION),
                        bitset.previousClearBit(POSITION),
                        bitset.nextSetBit(POSITION),
                        bitset.previousSetBit(POSITION)
                    });
            } catch(Exception e) {
                fail(e);
            }
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
