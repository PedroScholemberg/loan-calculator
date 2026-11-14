package com.loancalculator.domain;

import java.math.BigDecimal;

public class LoanValues {
    private BigDecimal loanAmount;
    private BigDecimal outstandingBalance;

    public LoanValues() {
    }

    public LoanValues(BigDecimal loanAmount, BigDecimal outstandingBalance) {
        this.loanAmount = loanAmount;
        this.outstandingBalance = outstandingBalance;
    }

    public BigDecimal getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(BigDecimal loanAmount) {
        this.loanAmount = loanAmount;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }
}
