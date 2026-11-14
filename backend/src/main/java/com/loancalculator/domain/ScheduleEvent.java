package com.loancalculator.domain;

import java.time.LocalDate;

public class ScheduleEvent {
    private LocalDate date;
    private boolean loanStart;
    private boolean monthEnd;
    private boolean payment;
    private Integer installmentNumber;

    public ScheduleEvent() {
    }

    public ScheduleEvent(LocalDate date, boolean loanStart, boolean monthEnd, boolean payment, Integer installmentNumber) {
        this.date = date;
        this.loanStart = loanStart;
        this.monthEnd = monthEnd;
        this.payment = payment;
        this.installmentNumber = installmentNumber;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isLoanStart() {
        return loanStart;
    }

    public void setLoanStart(boolean loanStart) {
        this.loanStart = loanStart;
    }

    public boolean isMonthEnd() {
        return monthEnd;
    }

    public void setMonthEnd(boolean monthEnd) {
        this.monthEnd = monthEnd;
    }

    public boolean isPayment() {
        return payment;
    }

    public void setPayment(boolean payment) {
        this.payment = payment;
    }

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(Integer installmentNumber) {
        this.installmentNumber = installmentNumber;
    }
}
