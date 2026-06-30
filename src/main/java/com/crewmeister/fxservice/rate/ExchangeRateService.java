package com.crewmeister.fxservice.rate;

import com.crewmeister.fxservice.currency.CurrencyDTO;
import com.crewmeister.fxservice.currency.CurrencyMapper;
import com.crewmeister.fxservice.currency.CurrencyRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ExchangeRateService {

    private final CurrencyRepository currencyRepository;
    private final CurrencyMapper currencyMapper;

    public ExchangeRateService(CurrencyRepository currencyRepository, CurrencyMapper currencyMapper) {
        this.currencyRepository = currencyRepository;
        this.currencyMapper = currencyMapper;
    }

    public List<CurrencyDTO> listCurrencies() {
        return currencyRepository.findAll(Sort.by("code")).stream()
                .map(currencyMapper::toDto)
                .toList();
    }
}
