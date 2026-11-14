package com.loancalculator.domain;

import java.util.List;

public class LoanSimulationResult {
    private int installmentCount;
    private List<LoanCalculationRow> rows;

    public LoanSimulationResult() {
    }

    public LoanSimulationResult(int installmentCount, List<LoanCalculationRow> rows) {
        this.installmentCount = installmentCount;
        this.rows = rows;
    }

    public int getInstallmentCount() {
        return installmentCount;
    }

    public void setInstallmentCount(int installmentCount) {
        this.installmentCount = installmentCount;
    }

    public List<LoanCalculationRow> getRows() {
        return rows;
    }

    public void setRows(List<LoanCalculationRow> rows) {
        this.rows = rows;
    }
}
