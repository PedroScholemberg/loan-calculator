package com.loancalculator.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AmortizationScheduleRequest(
        @NotNull(message = "Informe a data inicial.") LocalDate startDate,
        @NotNull(message = "Informe a data final.") LocalDate endDate,
        @NotNull(message = "Informe a data do primeiro pagamento.") LocalDate firstPaymentDate,
        @NotNull(message = "Informe o valor do empréstimo.")
        @DecimalMin(value = "0", inclusive = false,
                message = "O valor do empréstimo deve ser maior que zero.") BigDecimal principal,
        @NotNull(message = "Informe a taxa de juros anual.")
        @DecimalMin(value = "0", message = "A taxa de juros não pode ser negativa.") BigDecimal annualInterestRate) {

    @AssertTrue(message = "A data final deve ser posterior à data inicial.")
    public boolean isEndDateValid() {
        return startDate == null || endDate == null || endDate.isAfter(startDate);
    }

    @AssertTrue(message = "O primeiro pagamento deve ser posterior à data inicial.")
    public boolean isFirstPaymentAfterStart() {
        return startDate == null || firstPaymentDate == null || firstPaymentDate.isAfter(startDate);
    }

    @AssertTrue(message = "O primeiro pagamento deve ser anterior à data final.")
    public boolean isFirstPaymentBeforeEnd() {
        return endDate == null || firstPaymentDate == null || firstPaymentDate.isBefore(endDate);
    }

    @AssertTrue(message = "Informe o valor com no máximo 2 casas decimais.")
    public boolean isPrincipalPrecisionValid() {
        return principal == null || principal.stripTrailingZeros().scale() <= 2;
    }
}
