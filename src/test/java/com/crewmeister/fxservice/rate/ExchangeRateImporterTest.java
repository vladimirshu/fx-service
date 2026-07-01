package com.crewmeister.fxservice.rate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crewmeister.fxservice.currency.Currency;
import com.crewmeister.fxservice.currency.CurrencyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExchangeRateImporterTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private BundesbankRestClient bundesbankRestClient;

    private ExchangeRateImporter exchangeRateImporter;

    @BeforeEach
    void setUp() {
        exchangeRateImporter = new ExchangeRateImporter(currencyRepository, exchangeRateRepository, bundesbankRestClient);
    }

    @Test
    void importExchangeRatesAndCurrenciesStoresCurrenciesAndRatesFromBundesbankResponse() {
        when(bundesbankRestClient.getCurrencies()).thenReturn(List.of(
                new ImportedCurrency("CHF", "Swiss franc"),
                new ImportedCurrency("USD", "US dollar")
        ));
        when(bundesbankRestClient.getExchangeRates("CHF")).thenReturn(List.of(
                new ImportedExchangeRate("CHF", LocalDate.of(2026, 6, 29), new BigDecimal("0.9351"))
        ));
        when(bundesbankRestClient.getExchangeRates("USD")).thenReturn(List.of(
                new ImportedExchangeRate("USD", LocalDate.of(2026, 6, 29), new BigDecimal("1.1406")),
                new ImportedExchangeRate("USD", LocalDate.of(2026, 6, 30), new BigDecimal("1.1394"))
        ));

        exchangeRateImporter.importExchangeRatesAndCurrencies();

        verify(currencyRepository).saveAll(argThat(currencies -> {
            assertThat(currencies).extracting(Currency::getCode, Currency::getName)
                    .containsExactly(tuple("CHF", "Swiss franc"), tuple("USD", "US dollar"));
            return true;
        }));
        verify(exchangeRateRepository).saveAll(argThat(exchangeRates -> {
            assertThat(exchangeRates).extracting(
                    exchangeRate -> exchangeRate.getCurrency().getCode(),
                    ExchangeRate::getDate,
                    ExchangeRate::getRate
            ).containsExactly(
                    tuple("CHF", LocalDate.of(2026, 6, 29), new BigDecimal("0.9351")),
                    tuple("USD", LocalDate.of(2026, 6, 29), new BigDecimal("1.1406")),
                    tuple("USD", LocalDate.of(2026, 6, 30), new BigDecimal("1.1394"))
            );
            return true;
        }));
    }

    @Test
    void updateExchangeRatesAddsNewCurrenciesAndRemovesUnavailableCurrencies() {
        when(bundesbankRestClient.getCurrencies()).thenReturn(List.of(
                new ImportedCurrency("CHF", "Swiss franc"),
                new ImportedCurrency("JPY", "Japanese yen"),
                new ImportedCurrency("USD", "US dollar")
        ));
        when(currencyRepository.findAll()).thenReturn(List.of(
                new Currency("CAD", "Canadian dollar"),
                new Currency("CHF", "Swiss franc")
        ));

        exchangeRateImporter.updateExchangeRates();

        verify(currencyRepository).saveAll(argThat(currencies -> {
            assertThat(currencies).extracting(Currency::getCode, Currency::getName)
                    .containsExactly(tuple("JPY", "Japanese yen"), tuple("USD", "US dollar"));
            return true;
        }));
        verify(currencyRepository).deleteAllById(List.of("CAD"));
    }
}
