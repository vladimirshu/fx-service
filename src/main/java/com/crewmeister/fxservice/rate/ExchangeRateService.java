package com.crewmeister.fxservice.rate;

import com.crewmeister.fxservice.currency.CurrencyDTO;
import com.crewmeister.fxservice.currency.CurrencyMapper;
import com.crewmeister.fxservice.currency.CurrencyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

    public List<CurrencyDTO> listCurrencies() {
        return currencyRepository.findAll(Sort.by("code")).stream()
                .map(currencyMapper::toDto)
                .toList();
    }

    public List<ExchangeRateDTO> listExchangeRates() {
        return exchangeRateRepository.findAllOrderedByCurrencyCodeAndDate().stream()
                .map(exchangeRateMapper::toDto)
                .toList();
    }

    public Optional<BigDecimal> getExchangeRate(String currencyCode, LocalDate date) {
        return exchangeRateRepository.findByCurrencyCodeAndDate(currencyCode, date)
                .map(ExchangeRate::getRate);
    }
}
