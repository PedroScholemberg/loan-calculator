package com.loancalculator.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ScheduleEntry(LocalDate date, ScheduleEventType eventType, Integer installmentNumber,
        BigDecimal loanAmount, BigDecimal outstandingBalance, BigDecimal totalPayment,
        BigDecimal amortization, BigDecimal remainingPrincipal, BigDecimal interestProvision,
        BigDecimal accumulatedInterest, BigDecimal paidInterest) {
}
