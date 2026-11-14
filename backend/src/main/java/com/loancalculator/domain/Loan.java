package com.loancalculator.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Loan {
    LocalDate startDate;
    LocalDate endDate;
    LocalDate firstPaymentDate;
    BigDecimal principal;

    public Loan() {
    }

    public Loan(LocalDate startDate, LocalDate endDate, LocalDate firstPaymentDate, BigDecimal principal) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.firstPaymentDate = firstPaymentDate;
        this.principal = principal;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getFirstPaymentDate() {
        return firstPaymentDate;
    }

    public void setFirstPaymentDate(LocalDate firstPaymentDate) {
        this.firstPaymentDate = firstPaymentDate;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public void setPrincipal(BigDecimal principal) {
        this.principal = principal;
    }
}
