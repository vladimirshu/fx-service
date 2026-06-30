package com.crewmeister.fxservice.rate;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateImportJob {

    private final ExchangeRateImporter exchangeRateImporter;

    public ExchangeRateImportJob(ExchangeRateImporter exchangeRateImporter) {
        this.exchangeRateImporter = exchangeRateImporter;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startInitialImport() {
        exchangeRateImporter.importExchangeRates();
    }
}
