package com.crewmeister.fxservice.rate.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.crewmeister.fxservice.currency.CurrencyRepository;
import com.crewmeister.fxservice.rate.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

@ExtendWith(MockitoExtension.class)
class ExchangeRateDataHealthIndicatorTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    private ExchangeRateDataHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new ExchangeRateDataHealthIndicator(currencyRepository, exchangeRateRepository);
    }

    @Test
    void healthIsUpWhenCurrenciesAndExchangeRatesExist() {
        when(currencyRepository.count()).thenReturn(2L);
        when(exchangeRateRepository.count()).thenReturn(10L);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("currencies", 2L)
                .containsEntry("exchangeRates", 10L);
    }

    @Test
    void healthIsDownWhenCurrenciesAreMissing() {
        when(currencyRepository.count()).thenReturn(0L);
        when(exchangeRateRepository.count()).thenReturn(10L);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    void healthIsDownWhenExchangeRatesAreMissing() {
        when(currencyRepository.count()).thenReturn(2L);
        when(exchangeRateRepository.count()).thenReturn(0L);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    }
}
