package com.crewmeister.fxservice.rate;

import com.crewmeister.fxservice.currency.Currency;
import com.crewmeister.fxservice.currency.CurrencyRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateImporter {

    private final CurrencyRepository currencyRepository;
    private final BundesbankRestClient bundesbankRestClient;

    public ExchangeRateImporter(CurrencyRepository currencyRepository, BundesbankRestClient bundesbankRestClient) {
        this.currencyRepository = currencyRepository;
        this.bundesbankRestClient = bundesbankRestClient;
    }

    public void importExchangeRates() {
        List<Currency> currencies = bundesbankRestClient.getCurrencies().stream()
                .map(currency -> new Currency(currency.code(), currency.name()))
                .toList();

        currencyRepository.saveAll(currencies);
    }
}
