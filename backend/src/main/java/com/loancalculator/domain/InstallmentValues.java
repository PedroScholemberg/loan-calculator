package com.loancalculator.domain;

import java.math.BigDecimal;

public class InstallmentValues {
    private BigDecimal total;

    public InstallmentValues() {
    }

    public InstallmentValues(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }
}
