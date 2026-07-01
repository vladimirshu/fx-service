package com.crewmeister.fxservice.rate;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

        verify(exchangeRateImporter).updateExchangeRates();
    }
}
