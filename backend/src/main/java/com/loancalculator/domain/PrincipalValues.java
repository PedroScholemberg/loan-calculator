package com.loancalculator.domain;

import java.math.BigDecimal;

public class PrincipalValues {
    private BigDecimal amortization;
    private BigDecimal remainingBalance;

    public PrincipalValues() {
    }

    public PrincipalValues(BigDecimal amortization, BigDecimal remainingBalance) {
        this.amortization = amortization;
        this.remainingBalance = remainingBalance;
    }

    public BigDecimal getAmortization() {
        return amortization;
    }

    public void setAmortization(BigDecimal amortization) {
        this.amortization = amortization;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }
}
