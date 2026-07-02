package com.crewmeister.fxservice.rate.importer;

import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;

@ExtendWith(MockitoExtension.class)
class ExchangeRateImportJobTest {

    @Mock
    private ExchangeRateImporter exchangeRateImporter;

    private ExchangeRateImportJob exchangeRateImportJob;

    @BeforeEach
    void setUp() {
        exchangeRateImportJob = new ExchangeRateImportJob(exchangeRateImporter);
    }

    @Test
    void startInitialImportDelegatesToImporter() {
        exchangeRateImportJob.startInitialImport();

        verify(exchangeRateImporter).importExchangeRatesAndCurrencies();
    }

    @Test
    void updateExchangeRatesDailyDelegatesToImporter() {
        exchangeRateImportJob.updateExchangeRatesDaily();

        verify(exchangeRateImporter).updateExchangeRatesAndCurrencies();
    }
}
