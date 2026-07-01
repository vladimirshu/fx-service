package com.crewmeister.fxservice.rate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crewmeister.fxservice.currency.Currency;
import com.crewmeister.fxservice.currency.CurrencyRepository;
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
    private BundesbankRestClient bundesbankRestClient;

    private ExchangeRateImporter exchangeRateImporter;

    @BeforeEach
    void setUp() {
        exchangeRateImporter = new ExchangeRateImporter(currencyRepository, bundesbankRestClient);
    }

    @Test
    void importExchangeRatesStoresCurrenciesFromBundesbankResponse() {
        when(bundesbankRestClient.getCurrencies()).thenReturn(List.of(
                new ImportedCurrency("CHF", "Swiss franc"),
                new ImportedCurrency("USD", "US dollar")
        ));

        exchangeRateImporter.importExchangeRates();

        verify(currencyRepository).saveAll(argThat(currencies -> {
            assertThat(currencies).extracting(Currency::getCode, Currency::getName)
                    .containsExactly(tuple("CHF", "Swiss franc"), tuple("USD", "US dollar"));
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
