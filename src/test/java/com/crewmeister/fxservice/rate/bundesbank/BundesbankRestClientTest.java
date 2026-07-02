package com.crewmeister.fxservice.rate.bundesbank;

import static org.assertj.core.api.Assertions.assertThat;

import com.crewmeister.fxservice.rate.importer.ImportedCurrency;
import com.crewmeister.fxservice.rate.importer.ImportedExchangeRate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BundesbankRestClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final BundesbankRestClient bundesbankRestClient = new BundesbankRestClient();

    @Test
    void parseCurrenciesMapsCurrenciesFromSdmxSeriesDimension() throws IOException {
        JsonNode response = readFixture("currencies-response.json");

        List<ImportedCurrency> currencies = bundesbankRestClient.parseCurrencies(response);

        assertThat(currencies).containsExactly(
                new ImportedCurrency("CHF", "Swiss franc"),
                new ImportedCurrency("USD", "US dollar")
        );
    }

    @Test
    void parseExchangeRatesMapsNonNullObservationsFromSdmxDataSet() throws IOException {
        JsonNode response = readFixture("exchange-rates-response.json");

        List<ImportedExchangeRate> exchangeRates = bundesbankRestClient.parseExchangeRates("USD", response);

        assertThat(exchangeRates).containsExactly(
                new ImportedExchangeRate("USD", LocalDate.of(2026, 6, 22), new BigDecimal("1.1456")),
                new ImportedExchangeRate("USD", LocalDate.of(2026, 6, 23), new BigDecimal("1.1392")),
                new ImportedExchangeRate("USD", LocalDate.of(2026, 6, 24), new BigDecimal("1.1340")),
                new ImportedExchangeRate("USD", LocalDate.of(2026, 6, 25), new BigDecimal("1.1342")),
                new ImportedExchangeRate("USD", LocalDate.of(2026, 6, 26), new BigDecimal("1.1401")),
                new ImportedExchangeRate("USD", LocalDate.of(2026, 6, 29), new BigDecimal("1.1406")),
                new ImportedExchangeRate("USD", LocalDate.of(2026, 6, 30), new BigDecimal("1.1394"))
        );
    }

    private JsonNode readFixture(String fileName) throws IOException {
        try (InputStream fixture = getClass().getResourceAsStream("/bundesbank/" + fileName)) {
            return OBJECT_MAPPER.readTree(fixture);
        }
    }
}
