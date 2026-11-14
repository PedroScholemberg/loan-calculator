package com.loancalculator.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FinancialState {
    private LocalDate previousDate;
    private BigDecimal remainingPrincipal;
    private BigDecimal accumulatedInterest;

    public FinancialState() {
    }

    public FinancialState(LocalDate previousDate, BigDecimal remainingPrincipal, BigDecimal accumulatedInterest) {
        this.previousDate = previousDate;
        this.remainingPrincipal = remainingPrincipal;
        this.accumulatedInterest = accumulatedInterest;
    }

    public LocalDate getPreviousDate() {
        return previousDate;
    }

    public void setPreviousDate(LocalDate previousDate) {
        this.previousDate = previousDate;
    }

    public BigDecimal getRemainingPrincipal() {
        return remainingPrincipal;
    }

    public void setRemainingPrincipal(BigDecimal remainingPrincipal) {
        this.remainingPrincipal = remainingPrincipal;
    }

    public BigDecimal getAccumulatedInterest() {
        return accumulatedInterest;
    }

    public void setAccumulatedInterest(BigDecimal accumulatedInterest) {
        this.accumulatedInterest = accumulatedInterest;
    }
}
