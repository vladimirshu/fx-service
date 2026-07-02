package com.crewmeister.fxservice.rate.importer;

import com.crewmeister.fxservice.currency.Currency;
import com.crewmeister.fxservice.currency.CurrencyRepository;
import com.crewmeister.fxservice.rate.ExchangeRate;
import com.crewmeister.fxservice.rate.ExchangeRateRepository;
import com.crewmeister.fxservice.rate.bundesbank.BundesbankRestClient;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeRateImporter.class);

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
    @CacheEvict(
            cacheNames = {"currencies", "exchangeRates", "exchangeRateByCurrencyAndDate"},
            allEntries = true
    )
    public void importExchangeRatesAndCurrencies() {
        if (hasImportedData()) {
            LOGGER.info("Initial exchange rate import skipped because data already exists");
            return;
        }

        List<ImportedCurrency> importedCurrencies = bundesbankRestClient.getCurrencies();
        int importedCurrencyCount = importCurrencies(importedCurrencies);

        int importedExchangeRateCount = importExchangeRates();
        LOGGER.info(
                "Imported {} currencies and {} exchange rates",
                importedCurrencyCount,
                importedExchangeRateCount
        );
        LOGGER.info("Initial exchange rate import completed successfully");
    }

    @Transactional
    @CacheEvict(
            cacheNames = {"currencies", "exchangeRates", "exchangeRateByCurrencyAndDate"},
            allEntries = true
    )
    public void updateExchangeRatesAndCurrencies() {
        Map<String, ImportedCurrency> importedCurrenciesByCode = getImportedCurrenciesByCode();
        Set<String> existingCurrencyCodes = getExistingCurrencyCodes();

        int importedCurrencyCount = addNewCurrencies(importedCurrenciesByCode, existingCurrencyCodes);
        removeUnavailableCurrencies(importedCurrenciesByCode, existingCurrencyCodes);

        int importedExchangeRateCount = importExchangeRates(LocalDate.now().minusDays(1));
        LOGGER.info(
                "Imported {} currencies and {} exchange rates",
                importedCurrencyCount,
                importedExchangeRateCount
        );
    }

    private int importCurrencies(List<ImportedCurrency> importedCurrencies) {
        LOGGER.debug("Parsed {} currency records before persistence", importedCurrencies.size());
        List<Currency> currencies = importedCurrencies.stream()
                .map(this::toCurrency)
                .toList();

        currencyRepository.saveAll(currencies);
        return currencies.size();
    }

    private boolean hasImportedData() {
        return currencyRepository.count() > 0 && exchangeRateRepository.count() > 0;
    }

    private int importExchangeRates() {
        return importExchangeRates(null);
    }

    private int importExchangeRates(LocalDate date) {
        List<Currency> currencies = currencyRepository.findAll();
        List<ExchangeRate> exchangeRates = currencies.stream()
                .flatMap(currency -> getExchangeRatesFor(currency, date))
                .toList();

        LOGGER.debug("Parsed {} exchange rate records before persistence", exchangeRates.size());
        exchangeRateRepository.saveAll(exchangeRates);
        return exchangeRates.size();
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

    private int addNewCurrencies(
            Map<String, ImportedCurrency> importedCurrenciesByCode,
            Set<String> existingCurrencyCodes
    ) {
        List<Currency> currenciesToAdd = importedCurrenciesByCode.entrySet().stream()
                .filter(entry -> !existingCurrencyCodes.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .map(this::toCurrency)
                .toList();

        currencyRepository.saveAll(currenciesToAdd);
        return currenciesToAdd.size();
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
