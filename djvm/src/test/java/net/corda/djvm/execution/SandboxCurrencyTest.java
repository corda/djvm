package net.corda.djvm.execution;

import net.corda.djvm.TestBase;
import net.corda.djvm.WithJava;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Currency;
import java.util.function.Function;

import static net.corda.djvm.SandboxType.JAVA;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SandboxCurrencyTest extends TestBase {
    SandboxCurrencyTest() {
        super(JAVA);
    }

    @ParameterizedTest
    @CsvSource({"GBP,British Pound Sterling,GBP,2", "EUR,Euro,EUR,2", "USD,US Dollar,USD,2"})
    void testCurrencies(String currencyCode, String displayName, String symbol, int fractionDigits) {
        parentedSandbox(ctx -> {
            SandboxExecutor<String, Object[]> executor = new DeterministicSandboxExecutor<>(ctx.getConfiguration());
            ExecutionSummaryWithResult<Object[]> output = WithJava.run(executor, GetCurrency.class, currencyCode);
            assertThat(output.getResult()).isEqualTo(new Object[]{ displayName, symbol, fractionDigits });
            return null;
        });
    }

    public static class GetCurrency implements Function<String, Object[]> {
        @Override
        public Object[] apply(String currencyCode) {
            Currency currency = Currency.getInstance(currencyCode);
            return new Object[] {
                currency.getDisplayName(),
                currency.getSymbol(),
                currency.getDefaultFractionDigits()
            };
        }
    }
}
