package com.crewmeister.fxservice.rate.importer;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ImportedExchangeRate(String currencyCode, LocalDate date, BigDecimal rate) {
}
