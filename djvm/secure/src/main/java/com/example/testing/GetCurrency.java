package com.example.testing;

import java.util.Currency;
import java.util.function.Function;

public class GetCurrency implements Function<String, Object[]> {
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

