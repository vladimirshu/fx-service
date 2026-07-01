package com.crewmeister.fxservice.rate;

import com.crewmeister.fxservice.currency.CurrencyRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("exchangeRateDataHealthIndicator")
public class ExchangeRateDataHealthIndicator implements HealthIndicator {

    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    public ExchangeRateDataHealthIndicator(
            CurrencyRepository currencyRepository,
            ExchangeRateRepository exchangeRateRepository
    ) {
        this.currencyRepository = currencyRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @Override
    public Health health() {
        long currencyCount = currencyRepository.count();
        long exchangeRateCount = exchangeRateRepository.count();

        Health.Builder health = currencyCount > 0 && exchangeRateCount > 0
                ? Health.up()
                : Health.down();

        return health
                .withDetail("currencies", currencyCount)
                .withDetail("exchangeRates", exchangeRateCount)
                .build();
    }
}
