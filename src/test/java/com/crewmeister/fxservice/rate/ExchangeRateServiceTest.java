package com.crewmeister.fxservice.rate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crewmeister.fxservice.currency.Currency;
import com.crewmeister.fxservice.currency.CurrencyDTO;
import com.crewmeister.fxservice.currency.CurrencyMapper;
import com.crewmeister.fxservice.currency.CurrencyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

    private static final List<ExchangeRate> EXCHANGE_RATES = List.of(
            new ExchangeRate(CURRENCIES.get(0), LocalDate.of(2026, 6, 29), new BigDecimal("0.934500")),
            new ExchangeRate(CURRENCIES.get(0), LocalDate.of(2026, 6, 30), new BigDecimal("0.936100")),
            new ExchangeRate(CURRENCIES.get(1), LocalDate.of(2026, 6, 30), new BigDecimal("1.095600"))
    );

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    private final CurrencyMapper currencyMapper = new CurrencyMapper();

    private final ExchangeRateMapper exchangeRateMapper = new ExchangeRateMapper();

    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        exchangeRateService = new ExchangeRateService(
                currencyRepository,
                currencyMapper,
                exchangeRateRepository,
                exchangeRateMapper
        );
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

    @Test
    void listExchangeRatesReturnsAllExchangeRatesAsDtos() {
        when(exchangeRateRepository.findAllOrderedByCurrencyCodeAndDate()).thenReturn(EXCHANGE_RATES);

        List<ExchangeRateDTO> exchangeRates = exchangeRateService.listExchangeRates();

        assertThat(exchangeRates).containsExactly(
                new ExchangeRateDTO("CHF", LocalDate.of(2026, 6, 29), new BigDecimal("0.934500")),
                new ExchangeRateDTO("CHF", LocalDate.of(2026, 6, 30), new BigDecimal("0.936100")),
                new ExchangeRateDTO("USD", LocalDate.of(2026, 6, 30), new BigDecimal("1.095600"))
        );
        verify(exchangeRateRepository).findAllOrderedByCurrencyCodeAndDate();
    }

    @Test
    void getExchangeRateReturnsRateForCurrencyAndDate() {
        LocalDate date = LocalDate.of(2026, 6, 30);
        when(exchangeRateRepository.findByCurrencyCodeAndDate("USD", date))
                .thenReturn(Optional.of(EXCHANGE_RATES.get(2)));

        Optional<BigDecimal> exchangeRate = exchangeRateService.getExchangeRate("USD", date);

        assertThat(exchangeRate).contains(new BigDecimal("1.095600"));
        verify(exchangeRateRepository).findByCurrencyCodeAndDate("USD", date);
    }

    @Test
    void getExchangeRateReturnsEmptyWhenNoRateExists() {
        LocalDate date = LocalDate.of(2026, 6, 28);
        when(exchangeRateRepository.findByCurrencyCodeAndDate("USD", date)).thenReturn(Optional.empty());

        Optional<BigDecimal> exchangeRate = exchangeRateService.getExchangeRate("USD", date);

        assertThat(exchangeRate).isEmpty();
        verify(exchangeRateRepository).findByCurrencyCodeAndDate("USD", date);
    }

    @Test
    void convertToEuroReturnsConvertedAmountRoundedToCents() {
        LocalDate date = LocalDate.of(2026, 6, 30);
        when(exchangeRateRepository.findByCurrencyCodeAndDate("USD", date))
                .thenReturn(Optional.of(EXCHANGE_RATES.get(2)));

        Optional<BigDecimal> convertedAmount = exchangeRateService.convertToEuro(
                "USD",
                date,
                new BigDecimal("100.00")
        );

        assertThat(convertedAmount).contains(new BigDecimal("91.27"));
        verify(exchangeRateRepository).findByCurrencyCodeAndDate("USD", date);
    }

    @Test
    void convertToEuroReturnsEmptyWhenNoRateExists() {
        LocalDate date = LocalDate.of(2026, 6, 28);
        when(exchangeRateRepository.findByCurrencyCodeAndDate("USD", date)).thenReturn(Optional.empty());

        Optional<BigDecimal> convertedAmount = exchangeRateService.convertToEuro(
                "USD",
                date,
                new BigDecimal("100.00")
        );

        assertThat(convertedAmount).isEmpty();
        verify(exchangeRateRepository).findByCurrencyCodeAndDate("USD", date);
    }
}
