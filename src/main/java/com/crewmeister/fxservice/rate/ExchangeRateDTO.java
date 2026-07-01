package com.crewmeister.fxservice.rate;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateDTO(
        String currency,
        @JsonFormat(pattern = "dd-MM-yyyy") LocalDate date,
        BigDecimal rate
) {
}
