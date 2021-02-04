package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.rewiring.SandboxClassLoader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.fail;

class SandboxExecutorJavaTest extends TestBase {
    private static final int TX_ID = 101;

    SandboxExecutorJavaTest() {
        super(JAVA);
    }

    @Test
    void testTransaction() {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Function<? super Object, ? extends Function<? super Object, ?>> taskFactory = classLoader.createRawTaskFactory();
                Function<? super Object, ?> verifyTask = taskFactory.compose(classLoader.createSandboxFunction()).apply(ContractWrapper.class);

                Class<?> sandboxClass = classLoader.toSandboxClass(Transaction.class);
                Object sandboxTx = sandboxClass.getDeclaredConstructor(int.class).newInstance(TX_ID);

                assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> verifyTask.apply(sandboxTx))
                    .withMessageContaining("Contract constraint violated: txId=" + TX_ID);
            } catch (Throwable t) {
                fail(t);
            }
        });
    }

    public interface Contract {
        @SuppressWarnings("unused")
        void verify(Transaction tx);
    }

    public static class ContractImplementation implements Contract {
        @Override
        public void verify(@NotNull Transaction tx) {
            throw new IllegalArgumentException("Contract constraint violated: txId=" + tx.getId());
        }
    }

    public static class ContractWrapper implements Function<Transaction, Void> {
        @Override
        public Void apply(Transaction input) {
            new ContractImplementation().verify(input);
            return null;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class Transaction {
        private final int id;

        public Transaction(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }
}