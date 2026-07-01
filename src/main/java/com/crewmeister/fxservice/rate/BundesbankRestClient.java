package com.crewmeister.fxservice.rate;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
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
    private static final String CURRENCIES_DATA_PATH = "/rest/data/BBEX3/D..EUR.BB.AC.000";
    private static final String EXCHANGE_RATES_DATA_PATH_TEMPLATE = "/rest/data/BBEX3/D.%s.EUR.BB.AC.000";

    private final RestClient restClient;

    public BundesbankRestClient() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        restClient = restClientBuilder.baseUrl(BASE_URL).build();
    }

    public List<ImportedCurrency> getCurrencies() {
        String dateString = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(CURRENCIES_DATA_PATH)
                        .queryParam("startPeriod", dateString)
                        .queryParam("endPeriod", dateString)
                        .queryParam("format", "sdmx_json")
                        .build())
                .retrieve()
                .body(JsonNode.class);

        return parseCurrencies(response);
    }

    public List<ImportedExchangeRate> getExchangeRates(String currencyCode) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(EXCHANGE_RATES_DATA_PATH_TEMPLATE.formatted(currencyCode))
                        .queryParam("format", "sdmx_json")
                        .queryParam("detail", "dataonly")
                        .build())
                .retrieve()
                .body(JsonNode.class);

        return parseExchangeRates(currencyCode, response);
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

    List<ImportedExchangeRate> parseExchangeRates(String currencyCode, JsonNode response) {
        List<LocalDate> observationDates = parseObservationDates(response);
        List<ImportedExchangeRate> exchangeRates = new ArrayList<>();

        JsonNode dataSets = response.path("data").path("dataSets");
        for (JsonNode dataSet : dataSets) {
            JsonNode series = dataSet.path("series");
            series.fields().forEachRemaining(seriesEntry -> {
                JsonNode observations = seriesEntry.getValue().path("observations");
                observations.fields().forEachRemaining(observationEntry -> {
                    JsonNode observation = observationEntry.getValue();
                    JsonNode rate = observation.path(0);

                    if (rate.isMissingNode() || rate.isNull() || rate.asText().isBlank()) {
                        return;
                    }

                    int dateIndex = Integer.parseInt(observationEntry.getKey());
                    exchangeRates.add(new ImportedExchangeRate(
                            currencyCode,
                            observationDates.get(dateIndex),
                            new BigDecimal(rate.asText())
                    ));
                });
            });
        }

        return exchangeRates.stream()
                .sorted(Comparator.comparing(ImportedExchangeRate::date))
                .toList();
    }

    private List<LocalDate> parseObservationDates(JsonNode response) {
        JsonNode observationDimensions = response.path("data").path("structure").path("dimensions").path("observation");

        for (JsonNode dimension : observationDimensions) {
            if (!"TIME_PERIOD".equals(dimension.path("id").asText())) {
                continue;
            }

            List<LocalDate> dates = new ArrayList<>();
            for (JsonNode value : dimension.path("values")) {
                dates.add(LocalDate.parse(value.path("id").asText()));
            }
            return dates;
        }

        return List.of();
    }

}
