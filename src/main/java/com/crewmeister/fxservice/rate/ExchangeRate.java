package com.crewmeister.fxservice.rate;

import com.crewmeister.fxservice.currency.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "exchange_rate",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_exchange_rate_currency_date",
                columnNames = {"currency_code", "date"}
        )
)
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "currency_code",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_exchange_rate_currency")
    )
    private Currency currency;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal rate;

    protected ExchangeRate() {
    }

    public ExchangeRate(Currency currency, LocalDate date, BigDecimal rate) {
        this.currency = currency;
        this.date = date;
        this.rate = rate;
    }

    public Long getId() {
        return id;
    }

    public Currency getCurrency() {
        return currency;
    }

    public LocalDate getDate() {
        return date;
    }

    public BigDecimal getRate() {
        return rate;
    }
}
