package com.crewmeister.fxservice.rate;

import com.crewmeister.fxservice.currency.CurrencyDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping("/currency")
    public List<CurrencyDTO> listCurrencies() {
        return exchangeRateService.listCurrencies();
    }

    @GetMapping(value = "/rate", params = {"!currency", "!date"})
    public List<ExchangeRateDTO> listExchangeRates() {
        return exchangeRateService.listExchangeRates();
    }

    @GetMapping(value = "/rate", params = {"currency", "date"})
    public BigDecimal getExchangeRate(
            @RequestParam String currency,
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate date
    ) {
        return exchangeRateService.getExchangeRate(currency, date)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/convert")
    public BigDecimal convertToEuro(
            @RequestParam String currency,
            @RequestParam @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate date,
            @RequestParam BigDecimal amount
    ) {
        return exchangeRateService.convertToEuro(currency, date, amount)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping(value = "/rate", params = {"currency", "!date"})
    public void rejectCurrencyOnlyRateLookup() {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }

    @GetMapping(value = "/rate", params = {"!currency", "date"})
    public void rejectDateOnlyRateLookup() {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
}
