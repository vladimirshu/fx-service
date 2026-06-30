package com.crewmeister.fxservice.rate;

import com.crewmeister.fxservice.currency.Currency;
import com.crewmeister.fxservice.currency.CurrencyRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateImporter {

    private final CurrencyRepository currencyRepository;

    public ExchangeRateImporter(CurrencyRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    public void importExchangeRates() {
        List<Currency> currencies = List.of(
                new Currency("CHF", "Swiss franc"),
                new Currency("EUR", "Euro")
        );

        currencyRepository.saveAll(currencies);
    }
}
