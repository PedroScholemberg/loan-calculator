package com.loancalculator.domain;

import java.math.BigDecimal;

public class InterestValues {
    private BigDecimal provision;
    private BigDecimal accumulated;
    private BigDecimal paid;

    public InterestValues() {
    }

    public InterestValues(BigDecimal provision, BigDecimal accumulated, BigDecimal paid) {
        this.provision = provision;
        this.accumulated = accumulated;
        this.paid = paid;
    }

    public BigDecimal getProvision() {
        return provision;
    }

    public void setProvision(BigDecimal provision) {
        this.provision = provision;
    }

    public BigDecimal getAccumulated() {
        return accumulated;
    }

    public void setAccumulated(BigDecimal accumulated) {
        this.accumulated = accumulated;
    }

    public BigDecimal getPaid() {
        return paid;
    }

    public void setPaid(BigDecimal paid) {
        this.paid = paid;
    }
}
