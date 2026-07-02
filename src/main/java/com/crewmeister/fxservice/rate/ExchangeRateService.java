package com.crewmeister.fxservice.rate;

import com.crewmeister.fxservice.currency.CurrencyDTO;
import com.crewmeister.fxservice.currency.CurrencyMapper;
import com.crewmeister.fxservice.currency.CurrencyRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ExchangeRateService {

    private final CurrencyRepository currencyRepository;
    private final CurrencyMapper currencyMapper;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExchangeRateMapper exchangeRateMapper;

    public ExchangeRateService(
            CurrencyRepository currencyRepository,
            CurrencyMapper currencyMapper,
            ExchangeRateRepository exchangeRateRepository,
            ExchangeRateMapper exchangeRateMapper
    ) {
        this.currencyRepository = currencyRepository;
        this.currencyMapper = currencyMapper;
        this.exchangeRateRepository = exchangeRateRepository;
        this.exchangeRateMapper = exchangeRateMapper;
    }

    @Cacheable(cacheNames = "currencies")
    public List<CurrencyDTO> listCurrencies() {
        return currencyRepository.findAll(Sort.by("code")).stream()
                .map(currencyMapper::toDto)
                .toList();
    }

    @Cacheable(cacheNames = "exchangeRates")
    public List<ExchangeRateDTO> listExchangeRates() {
        return exchangeRateRepository.findAllOrderedByCurrencyCodeAndDate().stream()
                .map(exchangeRateMapper::toDto)
                .toList();
    }

    @Cacheable(cacheNames = "exchangeRateByCurrencyAndDate", unless = "#result == null")
    public Optional<BigDecimal> getExchangeRate(String currencyCode, LocalDate date) {
        return exchangeRateRepository.findByCurrencyCodeAndDate(currencyCode, date)
                .map(ExchangeRate::getRate);
    }

    public Optional<BigDecimal> convertToEuro(String currencyCode, LocalDate date, BigDecimal amount) {
        return getExchangeRate(currencyCode, date)
                .map(rate -> amount.divide(rate, 2, RoundingMode.HALF_UP));
    }
}
