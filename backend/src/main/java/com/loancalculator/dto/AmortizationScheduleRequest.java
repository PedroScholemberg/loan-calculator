package com.loancalculator.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AmortizationScheduleRequest(
        @Schema(description = "Loan start date (YYYY-MM-DD)", example = "2024-01-01")
        @NotNull(message = "Informe a data inicial.") LocalDate startDate,
        @Schema(description = "Loan maturity date, after the start date", example = "2034-01-01")
        @NotNull(message = "Informe a data final.") LocalDate endDate,
        @Schema(description = "First payment date, strictly between the start and end dates", example = "2024-02-15")
        @NotNull(message = "Informe a data do primeiro pagamento.") LocalDate firstPaymentDate,
        @Schema(description = "Loan principal, greater than zero with at most two decimal places", example = "140000")
        @NotNull(message = "Informe o valor do empréstimo.")
        @DecimalMin(value = "0", inclusive = false,
                message = "O valor do empréstimo deve ser maior que zero.") BigDecimal principal,
        @Schema(description = "Annual interest rate as a percentage; 7 means 7%. Zero is allowed", example = "7")
        @NotNull(message = "Informe a taxa de juros anual.")
        @DecimalMin(value = "0", message = "A taxa de juros não pode ser negativa.") BigDecimal annualInterestRate) {

    @AssertTrue(message = "A data final deve ser posterior à data inicial.")
    @Schema(hidden = true)
    public boolean isEndDateValid() {
        return startDate == null || endDate == null || endDate.isAfter(startDate);
    }

    @AssertTrue(message = "O primeiro pagamento deve ser posterior à data inicial.")
    @Schema(hidden = true)
    public boolean isFirstPaymentAfterStart() {
        return startDate == null || firstPaymentDate == null || firstPaymentDate.isAfter(startDate);
    }

    @AssertTrue(message = "O primeiro pagamento deve ser anterior à data final.")
    @Schema(hidden = true)
    public boolean isFirstPaymentBeforeEnd() {
        return endDate == null || firstPaymentDate == null || firstPaymentDate.isBefore(endDate);
    }

    @AssertTrue(message = "Informe o valor com no máximo 2 casas decimais.")
    @Schema(hidden = true)
    public boolean isPrincipalPrecisionValid() {
        return principal == null || principal.stripTrailingZeros().scale() <= 2;
    }
}
