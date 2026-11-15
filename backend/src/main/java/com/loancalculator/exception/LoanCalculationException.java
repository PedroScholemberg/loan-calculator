package com.loancalculator.exception;

public class LoanCalculationException extends RuntimeException {
    public LoanCalculationException() {
        super("Os parâmetros informados não permitem calcular um resultado financeiro válido.");
    }

    public LoanCalculationException(Throwable cause) {
        super("Os valores informados excedem a capacidade de cálculo. Revise os parâmetros.", cause);
    }
}
