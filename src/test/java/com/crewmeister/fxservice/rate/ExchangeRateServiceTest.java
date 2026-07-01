package com.crewmeister.fxservice.rate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crewmeister.fxservice.currency.Currency;
import com.crewmeister.fxservice.currency.CurrencyDTO;
import com.crewmeister.fxservice.currency.CurrencyMapper;
import com.crewmeister.fxservice.currency.CurrencyRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    private static final List<Currency> CURRENCIES = List.of(
            new Currency("CHF", "Swiss franc"),
            new Currency("USD", "US dollar")
    );

    @Mock
    private CurrencyRepository currencyRepository;

    private final CurrencyMapper currencyMapper = new CurrencyMapper();

    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        exchangeRateService = new ExchangeRateService(currencyRepository, currencyMapper);
    }

    @Test
    void listCurrenciesReturnsAllCurrenciesAsDtos() {
        when(currencyRepository.findAll(Sort.by("code"))).thenReturn(CURRENCIES);

        List<CurrencyDTO> currencies = exchangeRateService.listCurrencies();

        assertThat(currencies).containsExactly(
                new CurrencyDTO("CHF", "Swiss franc"),
                new CurrencyDTO("USD", "US dollar")
        );
        verify(currencyRepository).findAll(Sort.by("code"));
    }
}
