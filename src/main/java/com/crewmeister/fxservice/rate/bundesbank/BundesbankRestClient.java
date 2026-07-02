package com.crewmeister.fxservice.rate.bundesbank;

import com.crewmeister.fxservice.rate.importer.ImportedCurrency;
import com.crewmeister.fxservice.rate.importer.ImportedExchangeRate;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BundesbankRestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(BundesbankRestClient.class);

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
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> {
                        URI uri = uriBuilder
                                .path(CURRENCIES_DATA_PATH)
                                .queryParam("startPeriod", dateString)
                                .queryParam("endPeriod", dateString)
                                .queryParam("format", "sdmx_json")
                                .build();
                        LOGGER.debug("Resolved Bundesbank request URL: {}", uri);
                        return uri;
                    })
                    .retrieve()
                    .body(JsonNode.class);

            return parseCurrencies(response);
        } catch (RestClientException ex) {
            LOGGER.error("Bundesbank HTTP call failed", ex);
            throw ex;
        }
    }

    public List<ImportedExchangeRate> getExchangeRates(String currencyCode) {
        return getExchangeRates(currencyCode, null, null);
    }

    public List<ImportedExchangeRate> getExchangeRates(String currencyCode, LocalDate date) {
        return getExchangeRates(currencyCode, date, date);
    }

    private List<ImportedExchangeRate> getExchangeRates(String currencyCode, LocalDate startDate, LocalDate endDate) {
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> {
                        URI uri = uriBuilder
                                .path(EXCHANGE_RATES_DATA_PATH_TEMPLATE.formatted(currencyCode))
                                .queryParamIfPresent("startPeriod", formatDate(startDate))
                                .queryParamIfPresent("endPeriod", formatDate(endDate))
                                .queryParam("format", "sdmx_json")
                                .queryParam("detail", "dataonly")
                                .build();
                        LOGGER.debug("Resolved Bundesbank request URL: {}", uri);
                        return uri;
                    })
                    .retrieve()
                    .body(JsonNode.class);

            return parseExchangeRates(currencyCode, response);
        } catch (RestClientException ex) {
            LOGGER.error("Bundesbank HTTP call failed", ex);
            throw ex;
        }
    }

    private java.util.Optional<String> formatDate(LocalDate date) {
        return date == null
                ? java.util.Optional.empty()
                : java.util.Optional.of(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }

    List<ImportedCurrency> parseCurrencies(JsonNode response) {
        try {
            if (response == null || response.isMissingNode() || response.isNull()) {
                LOGGER.warn("Bundesbank response is empty or incomplete");
                return List.of();
            }

            JsonNode seriesDimensions = response.path("data").path("structure").path("dimensions").path("series");
            if (!seriesDimensions.isArray()) {
                LOGGER.warn("Bundesbank response is empty or incomplete");
                return List.of();
            }

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
        } catch (RuntimeException ex) {
            LOGGER.error("Bundesbank response cannot be parsed", ex);
            throw ex;
        }
    }

    List<ImportedExchangeRate> parseExchangeRates(String currencyCode, JsonNode response) {
        try {
            if (response == null || response.isMissingNode() || response.isNull()) {
                LOGGER.warn("Bundesbank response is empty or incomplete");
                return List.of();
            }

            List<LocalDate> observationDates = parseObservationDates(response);
            if (observationDates.isEmpty()) {
                LOGGER.warn("Bundesbank response is empty or incomplete");
                return List.of();
            }

            List<ImportedExchangeRate> exchangeRates = new ArrayList<>();

            JsonNode dataSets = response.path("data").path("dataSets");
            if (!dataSets.isArray()) {
                LOGGER.warn("Bundesbank response is empty or incomplete");
                return List.of();
            }

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
        } catch (RuntimeException ex) {
            LOGGER.error("Bundesbank response cannot be parsed", ex);
            throw ex;
        }
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
