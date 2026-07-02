package com.crewmeister.fxservice.rate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crewmeister.fxservice.currency.Currency;
import com.crewmeister.fxservice.currency.CurrencyDTO;
import com.crewmeister.fxservice.currency.CurrencyMapper;
import com.crewmeister.fxservice.currency.CurrencyRepository;
import com.crewmeister.fxservice.rate.bundesbank.BundesbankRestClient;
import com.crewmeister.fxservice.rate.importer.ExchangeRateImporter;
import com.crewmeister.fxservice.rate.importer.ImportedCurrency;
import com.crewmeister.fxservice.rate.importer.ImportedExchangeRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(ExchangeRateCachingTest.TestConfig.class)
class ExchangeRateCachingTest {

    private static final Currency USD = new Currency("USD", "US dollar");
    private static final Currency CHF = new Currency("CHF", "Swiss franc");
    private static final LocalDate DATE = LocalDate.of(2026, 6, 30);

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private ExchangeRateImporter exchangeRateImporter;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private BundesbankRestClient bundesbankRestClient;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        reset(currencyRepository, exchangeRateRepository, bundesbankRestClient);
        cacheManager.getCacheNames().forEach(cacheName -> cacheManager.getCache(cacheName).clear());
    }

    @Test
    void repeatedCurrencyListCallsUseCachedData() {
        when(currencyRepository.findAll(Sort.by("code"))).thenReturn(List.of(CHF, USD));

        List<CurrencyDTO> firstCall = exchangeRateService.listCurrencies();
        List<CurrencyDTO> secondCall = exchangeRateService.listCurrencies();

        assertThat(firstCall).containsExactly(
                new CurrencyDTO("CHF", "Swiss franc"),
                new CurrencyDTO("USD", "US dollar")
        );
        assertThat(secondCall).isEqualTo(firstCall);
        verify(currencyRepository, times(1)).findAll(Sort.by("code"));
    }

    @Test
    void repeatedExchangeRateListCallsUseCachedData() {
        ExchangeRate exchangeRate = new ExchangeRate(USD, DATE, new BigDecimal("1.095600"));
        when(exchangeRateRepository.findAllOrderedByCurrencyCodeAndDate()).thenReturn(List.of(exchangeRate));

        List<ExchangeRateDTO> firstCall = exchangeRateService.listExchangeRates();
        List<ExchangeRateDTO> secondCall = exchangeRateService.listExchangeRates();

        assertThat(firstCall).containsExactly(new ExchangeRateDTO("USD", DATE, new BigDecimal("1.095600")));
        assertThat(secondCall).isEqualTo(firstCall);
        verify(exchangeRateRepository, times(1)).findAllOrderedByCurrencyCodeAndDate();
    }

    @Test
    void repeatedExchangeRateByCurrencyAndDateCallsUseCachedData() {
        ExchangeRate exchangeRate = new ExchangeRate(USD, DATE, new BigDecimal("1.095600"));
        when(exchangeRateRepository.findByCurrencyCodeAndDate("USD", DATE)).thenReturn(Optional.of(exchangeRate));

        Optional<BigDecimal> firstCall = exchangeRateService.getExchangeRate("USD", DATE);
        Optional<BigDecimal> secondCall = exchangeRateService.getExchangeRate("USD", DATE);

        assertThat(firstCall).contains(new BigDecimal("1.095600"));
        assertThat(secondCall).isEqualTo(firstCall);
        verify(exchangeRateRepository, times(1)).findByCurrencyCodeAndDate("USD", DATE);
    }

    @Test
    void missingExchangeRateLookupIsNotCached() {
        when(exchangeRateRepository.findByCurrencyCodeAndDate("USD", DATE)).thenReturn(Optional.empty());

        Optional<BigDecimal> firstCall = exchangeRateService.getExchangeRate("USD", DATE);
        Optional<BigDecimal> secondCall = exchangeRateService.getExchangeRate("USD", DATE);

        assertThat(firstCall).isEmpty();
        assertThat(secondCall).isEmpty();
        verify(exchangeRateRepository, times(2)).findByCurrencyCodeAndDate("USD", DATE);
    }

    @Test
    void successfulInitialImportClearsCacheAndNextReadReturnsFreshData() {
        ExchangeRate oldRate = new ExchangeRate(USD, DATE, new BigDecimal("1.095600"));
        ExchangeRate freshRate = new ExchangeRate(USD, DATE, new BigDecimal("1.110000"));
        when(exchangeRateRepository.findByCurrencyCodeAndDate("USD", DATE))
                .thenReturn(Optional.of(oldRate), Optional.of(freshRate));
        when(bundesbankRestClient.getCurrencies()).thenReturn(List.of(new ImportedCurrency("USD", "US dollar")));
        when(currencyRepository.findAll()).thenReturn(List.of(USD));
        when(bundesbankRestClient.getExchangeRates("USD", null)).thenReturn(List.of(
                new ImportedExchangeRate("USD", DATE, new BigDecimal("1.110000"))
        ));

        assertThat(exchangeRateService.getExchangeRate("USD", DATE)).contains(new BigDecimal("1.095600"));

        exchangeRateImporter.importExchangeRatesAndCurrencies();

        assertThat(exchangeRateService.getExchangeRate("USD", DATE)).contains(new BigDecimal("1.110000"));
        verify(exchangeRateRepository, times(2)).findByCurrencyCodeAndDate("USD", DATE);
    }

    @Test
    void successfulDailyImportClearsCacheAndNextReadReturnsFreshData() {
        when(currencyRepository.findAll(Sort.by("code")))
                .thenReturn(List.of(USD), List.of(CHF, USD));
        when(bundesbankRestClient.getCurrencies()).thenReturn(List.of(
                new ImportedCurrency("CHF", "Swiss franc"),
                new ImportedCurrency("USD", "US dollar")
        ));
        when(currencyRepository.findAll()).thenReturn(List.of(USD), List.of(CHF, USD));
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(bundesbankRestClient.getExchangeRates("CHF", yesterday)).thenReturn(List.of());
        when(bundesbankRestClient.getExchangeRates("USD", yesterday)).thenReturn(List.of());

        assertThat(exchangeRateService.listCurrencies()).containsExactly(new CurrencyDTO("USD", "US dollar"));

        exchangeRateImporter.updateExchangeRatesAndCurrencies();

        assertThat(exchangeRateService.listCurrencies()).containsExactly(
                new CurrencyDTO("CHF", "Swiss franc"),
                new CurrencyDTO("USD", "US dollar")
        );
        verify(currencyRepository, times(2)).findAll(Sort.by("code"));
    }

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    "currencies",
                    "exchangeRates",
                    "exchangeRateByCurrencyAndDate"
            );
        }

        @Bean
        CurrencyRepository currencyRepository() {
            return mock(CurrencyRepository.class);
        }

        @Bean
        ExchangeRateRepository exchangeRateRepository() {
            return mock(ExchangeRateRepository.class);
        }

        @Bean
        BundesbankRestClient bundesbankRestClient() {
            return mock(BundesbankRestClient.class);
        }

        @Bean
        ExchangeRateService exchangeRateService(
                CurrencyRepository currencyRepository,
                ExchangeRateRepository exchangeRateRepository
        ) {
            return new ExchangeRateService(
                    currencyRepository,
                    new CurrencyMapper(),
                    exchangeRateRepository,
                    new ExchangeRateMapper()
            );
        }

        @Bean
        ExchangeRateImporter exchangeRateImporter(
                CurrencyRepository currencyRepository,
                ExchangeRateRepository exchangeRateRepository,
                BundesbankRestClient bundesbankRestClient
        ) {
            return new ExchangeRateImporter(currencyRepository, exchangeRateRepository, bundesbankRestClient);
        }
    }
}
