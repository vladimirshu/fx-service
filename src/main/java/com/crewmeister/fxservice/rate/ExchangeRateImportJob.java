package com.crewmeister.fxservice.rate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateImportJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeRateImportJob.class);

    private final ExchangeRateImporter exchangeRateImporter;

    public ExchangeRateImportJob(ExchangeRateImporter exchangeRateImporter) {
        this.exchangeRateImporter = exchangeRateImporter;
    }

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void startInitialImport() {
        LOGGER.info("Initial exchange rate import started");
        try {
            exchangeRateImporter.importExchangeRatesAndCurrencies();
        } catch (RuntimeException ex) {
            LOGGER.error("Initial exchange rate import failed", ex);
            throw ex;
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void updateExchangeRatesDaily() {
        LOGGER.info("Scheduled exchange rate update started");
        try {
            exchangeRateImporter.updateExchangeRatesAndCurrencies();
            LOGGER.info("Scheduled exchange rate update completed successfully");
        } catch (RuntimeException ex) {
            LOGGER.error("Scheduled exchange rate update failed", ex);
            throw ex;
        }
    }
}
