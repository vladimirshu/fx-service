package com.crewmeister.fxservice.rate;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
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

    private JsonNode readFixture(String fileName) throws IOException {
        try (InputStream fixture = getClass().getResourceAsStream("/bundesbank/" + fileName)) {
            return OBJECT_MAPPER.readTree(fixture);
        }
    }
}
