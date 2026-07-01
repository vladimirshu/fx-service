package com.crewmeister.fxservice.rate;

import com.crewmeister.fxservice.currency.Currency;
import com.crewmeister.fxservice.currency.CurrencyRepository;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    @Transactional
    public void updateExchangeRates() {
        Map<String, ImportedCurrency> importedCurrenciesByCode = bundesbankRestClient.getCurrencies().stream()
                .collect(Collectors.toMap(ImportedCurrency::code, Function.identity()));
        Set<String> existingCurrencyCodes = currencyRepository.findAll().stream()
                .map(Currency::getCode)
                .collect(Collectors.toSet());

        List<Currency> currenciesToAdd = importedCurrenciesByCode.entrySet().stream()
                .filter(entry -> !existingCurrencyCodes.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .map(currency -> new Currency(currency.code(), currency.name()))
                .toList();
        List<String> currencyCodesToRemove = existingCurrencyCodes.stream()
                .filter(code -> !importedCurrenciesByCode.containsKey(code))
                .toList();

        currencyRepository.saveAll(currenciesToAdd);
        currencyRepository.deleteAllById(currencyCodesToRemove);
    }
}
