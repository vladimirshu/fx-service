package com.crewmeister.fxservice.currency;

import org.springframework.stereotype.Component;

@Component
public class CurrencyMapper {

    public CurrencyDTO toDto(Currency currency) {
        return new CurrencyDTO(currency.getCode(), currency.getName());
    }
}
