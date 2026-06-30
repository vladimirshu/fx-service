package com.crewmeister.fxservice.rate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

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

    private ExchangeRateImporter exchangeRateImporter;

    @BeforeEach
    void setUp() {
        exchangeRateImporter = new ExchangeRateImporter(currencyRepository);
    }

    @Test
    void importExchangeRatesStoresCurrenciesFromMockedResponse() {
        exchangeRateImporter.importExchangeRates();

        verify(currencyRepository).saveAll(anyList());
    }
}
