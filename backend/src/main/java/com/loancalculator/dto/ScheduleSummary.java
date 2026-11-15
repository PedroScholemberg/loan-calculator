package com.loancalculator.dto;

import java.math.BigDecimal;

public record ScheduleSummary(BigDecimal totalInterest, BigDecimal totalAmortization, BigDecimal totalPayment) {
}
