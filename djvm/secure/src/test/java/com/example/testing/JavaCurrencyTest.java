package com.example.testing;

import net.corda.djvm.TypedTaskFactory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaCurrencyTest extends TestBase {
    @ParameterizedTest
    @CsvSource({
        "GBP,British Pound Sterling,GBP,2",
        "EUR,Euro,EUR,2",
        "USD,US Dollar,USD,2"
    })
    void testCurrencies(String currencyCode, String displayName, String symbol, int fractionDigits) {
        sandbox(ctx -> {
            try {
                TypedTaskFactory taskFactory = ctx.getClassLoader().createTypedTaskFactory();
                Object[] result = WithJava.run(taskFactory, GetCurrency.class, currencyCode);
                assertThat(result).isEqualTo(new Object[]{displayName, symbol, fractionDigits});
            } catch(Exception e) {
                fail(e);
            }
        });
    }
}
