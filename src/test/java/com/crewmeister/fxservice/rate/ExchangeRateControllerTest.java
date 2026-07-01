package com.crewmeister.fxservice.rate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.crewmeister.fxservice.currency.CurrencyDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExchangeRateController.class)
@Import(ExchangeRateControllerTest.TestConfig.class)
class ExchangeRateControllerTest {

    private static final List<CurrencyDTO> CURRENCIES = List.of(
            new CurrencyDTO("CHF", "Swiss franc"),
            new CurrencyDTO("USD", "US dollar")
    );

    private static final List<ExchangeRateDTO> EXCHANGE_RATES = List.of(
            new ExchangeRateDTO("CHF", LocalDate.of(2026, 6, 29), new BigDecimal("0.934500")),
            new ExchangeRateDTO("CHF", LocalDate.of(2026, 6, 30), new BigDecimal("0.936100")),
            new ExchangeRateDTO("USD", LocalDate.of(2026, 6, 30), new BigDecimal("1.095600"))
    );

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listCurrenciesReturnsAllAvailableCurrencies() throws Exception {
        mockMvc.perform(get("/api/v1/currency"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].code").value("CHF"))
                .andExpect(jsonPath("$[0].name").value("Swiss franc"))
                .andExpect(jsonPath("$[1].code").value("USD"))
                .andExpect(jsonPath("$[1].name").value("US dollar"));
    }

    @Test
    void listExchangeRatesReturnsAllAvailableExchangeRates() throws Exception {
        mockMvc.perform(get("/api/v1/rate"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].currency").value("CHF"))
                .andExpect(jsonPath("$[0].date").value("29-06-2026"))
                .andExpect(jsonPath("$[0].rate").value(0.934500))
                .andExpect(jsonPath("$[1].currency").value("CHF"))
                .andExpect(jsonPath("$[1].date").value("30-06-2026"))
                .andExpect(jsonPath("$[1].rate").value(0.936100))
                .andExpect(jsonPath("$[2].currency").value("USD"))
                .andExpect(jsonPath("$[2].date").value("30-06-2026"))
                .andExpect(jsonPath("$[2].rate").value(1.095600));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        ExchangeRateService exchangeRateService() {
            return new ExchangeRateService(null, null, null, null) {
                @Override
                public List<CurrencyDTO> listCurrencies() {
                    return CURRENCIES;
                }

                @Override
                public List<ExchangeRateDTO> listExchangeRates() {
                    return EXCHANGE_RATES;
                }
            };
        }
    }
}
