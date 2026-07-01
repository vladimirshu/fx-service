package com.crewmeister.fxservice.rate;

import com.crewmeister.fxservice.currency.Currency;
import com.crewmeister.fxservice.currency.CurrencyRepository;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

@Component
public class ExchangeRateImporter {

    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final BundesbankRestClient bundesbankRestClient;

    public ExchangeRateImporter(
            CurrencyRepository currencyRepository,
            ExchangeRateRepository exchangeRateRepository,
            BundesbankRestClient bundesbankRestClient
    ) {
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
        this.bundesbankRestClient = bundesbankRestClient;
    }

    @Transactional
    public void importExchangeRatesAndCurrencies() {
        List<ImportedCurrency> importedCurrencies = bundesbankRestClient.getCurrencies();
        importCurrencies(importedCurrencies);

        importExchangeRates();
    }

    @Transactional
    public void updateExchangeRatesAndCurrencies() {
        Map<String, ImportedCurrency> importedCurrenciesByCode = getImportedCurrenciesByCode();
        Set<String> existingCurrencyCodes = getExistingCurrencyCodes();

        addNewCurrencies(importedCurrenciesByCode, existingCurrencyCodes);
        removeUnavailableCurrencies(importedCurrenciesByCode, existingCurrencyCodes);

        importExchangeRates(LocalDate.now().minusDays(1));
    }

    private void importCurrencies(List<ImportedCurrency> importedCurrencies) {
        List<Currency> currencies = importedCurrencies.stream()
                .map(this::toCurrency)
                .toList();

        currencyRepository.saveAll(currencies);
    }

    private void importExchangeRates() {
        importExchangeRates(null);
    }

    private void importExchangeRates(LocalDate date) {
        List<Currency> currencies = currencyRepository.findAll();
        List<ExchangeRate> exchangeRates = currencies.stream()
                .flatMap(currency -> getExchangeRatesFor(currency, date))
                .toList();

        exchangeRateRepository.saveAll(exchangeRates);
    }

    private Map<String, ImportedCurrency> getImportedCurrenciesByCode() {
        return bundesbankRestClient.getCurrencies().stream()
                .collect(Collectors.toMap(ImportedCurrency::code, Function.identity()));
    }

    private Set<String> getExistingCurrencyCodes() {
        return currencyRepository.findAll().stream()
                .map(Currency::getCode)
                .collect(Collectors.toSet());
    }

    private void addNewCurrencies(
            Map<String, ImportedCurrency> importedCurrenciesByCode,
            Set<String> existingCurrencyCodes
    ) {
        List<Currency> currenciesToAdd = importedCurrenciesByCode.entrySet().stream()
                .filter(entry -> !existingCurrencyCodes.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .map(this::toCurrency)
                .toList();

        currencyRepository.saveAll(currenciesToAdd);
    }

    private void removeUnavailableCurrencies(
            Map<String, ImportedCurrency> importedCurrenciesByCode,
            Set<String> existingCurrencyCodes
    ) {
        List<String> currencyCodesToRemove = existingCurrencyCodes.stream()
                .filter(code -> !importedCurrenciesByCode.containsKey(code))
                .toList();

        currencyRepository.deleteAllById(currencyCodesToRemove);
    }

    private Stream<ExchangeRate> getExchangeRatesFor(Currency currency, LocalDate date) {
        return bundesbankRestClient.getExchangeRates(currency.getCode(), date).stream()
                .map(importedExchangeRate -> toExchangeRate(importedExchangeRate, currency));
    }

    private Currency toCurrency(ImportedCurrency importedCurrency) {
        return new Currency(importedCurrency.code(), importedCurrency.name());
    }

    private ExchangeRate toExchangeRate(ImportedExchangeRate importedExchangeRate, Currency currency) {
        return new ExchangeRate(
                currency,
                importedExchangeRate.date(),
                importedExchangeRate.rate()
        );
    }
}
