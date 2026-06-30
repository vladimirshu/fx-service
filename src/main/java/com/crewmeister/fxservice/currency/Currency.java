package com.crewmeister.fxservice.currency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "currency")
public class Currency {

    @Id
    @Column(length = 5, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    protected Currency() {
    }

    public Currency(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
