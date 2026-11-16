package com.loancalculator.dto;

import java.math.BigDecimal;

public record ScheduleSummary(int installmentCount, BigDecimal totalInterest, BigDecimal totalAmortization,
        BigDecimal totalPayment) {
}
