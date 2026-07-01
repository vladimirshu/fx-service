package com.crewmeister.fxservice.rate;

import org.springframework.stereotype.Component;

@Component
public class ExchangeRateMapper {

    public ExchangeRateDTO toDto(ExchangeRate exchangeRate) {
        return new ExchangeRateDTO(
                exchangeRate.getCurrency().getCode(),
                exchangeRate.getDate(),
                exchangeRate.getRate()
        );
    }
}
