package com.loancalculator.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ScheduleEntry(LocalDate date, ScheduleEventType eventType, BigDecimal openingBalance,
        BigDecimal interest, BigDecimal amortization, BigDecimal payment, BigDecimal closingBalance) {
}
