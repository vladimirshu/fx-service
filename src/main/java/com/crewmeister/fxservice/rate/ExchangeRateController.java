package com.crewmeister.fxservice.rate;

import com.crewmeister.fxservice.currency.CurrencyDTO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
