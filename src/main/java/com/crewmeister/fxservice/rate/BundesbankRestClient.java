package com.crewmeister.fxservice.rate;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BundesbankRestClient {

    private static final String BASE_URL = "https://api.statistiken.bundesbank.de";
    private static final String DATA_PATH = "/rest/data/BBEX3/D..EUR.BB.AC.000";

    private final RestClient restClient;

    public BundesbankRestClient() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        restClient = restClientBuilder.baseUrl(BASE_URL).build();
    }

    public List<ImportedCurrency> getCurrencies() {
        String dateString = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(DATA_PATH)
                        .queryParam("startPeriod", dateString)
                        .queryParam("endPeriod", dateString)
                        .queryParam("format", "sdmx_json")
                        .build())
                .retrieve()
                .body(JsonNode.class);

        return parseCurrencies(response);
    }

    List<ImportedCurrency> parseCurrencies(JsonNode response) {
        JsonNode seriesDimensions = response.path("data").path("structure").path("dimensions").path("series");
        List<ImportedCurrency> currencies = new ArrayList<>();

        for (JsonNode dimension : seriesDimensions) {
            if (!"BBK_STD_CURRENCY".equals(dimension.path("id").asText())) {
                continue;
            }

            for (JsonNode currency : dimension.path("values")) {
                String code = currency.path("id").asText();
                String name = currency.path("name").asText(code);

                if (!code.isBlank()) {
                    currencies.add(new ImportedCurrency(code, name));
                }
            }
        }

        return currencies.stream()
                .sorted(Comparator.comparing(ImportedCurrency::code))
                .toList();
    }

}
