package com.crewmeister.fxservice.rate;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    @Query("""
            select exchangeRate
            from ExchangeRate exchangeRate
            join fetch exchangeRate.currency currency
            order by currency.code, exchangeRate.date
            """)
    List<ExchangeRate> findAllOrderedByCurrencyCodeAndDate();
}
