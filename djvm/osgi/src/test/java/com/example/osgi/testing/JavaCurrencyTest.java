package com.example.osgi.testing;

import net.corda.djvm.rewiring.SandboxClassLoader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class JavaCurrencyTest extends TestBase {
    private static final Logger LOG = LoggerFactory.getLogger(JavaCurrencyTest.class);

    @ParameterizedTest
    @CsvSource({
        "GBP,British Pound Sterling,GBP,2",
        "EUR,Euro,EUR,2",
        "USD,US Dollar,USD,2"
    })
    void testCurrencies(String currencyCode, String displayName, String symbol, int fractionDigits) {
        sandbox(ctx -> {
            try {
                SandboxClassLoader classLoader = ctx.getClassLoader();
                Object[] result = WithJava.run(classLoader, "com.example.testing.GetCurrency", currencyCode);
                assertThat(result).isEqualTo(new Object[]{displayName, symbol, fractionDigits});
            } catch(Exception e) {
                LOG.error("Failed", e);
                fail(e);
            }
        });
    }
}
