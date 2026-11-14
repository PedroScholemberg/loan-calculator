package com.loancalculator.domain;

public class LoanCalculationRow {
    private ScheduleEvent event;
    private LoanValues loan;
    private InstallmentValues installment;
    private PrincipalValues principal;
    private InterestValues interest;

    public LoanCalculationRow() {
    }

    public LoanCalculationRow(ScheduleEvent event, LoanValues loan, InstallmentValues installment, PrincipalValues principal, InterestValues interest) {
        this.event = event;
        this.loan = loan;
        this.installment = installment;
        this.principal = principal;
        this.interest = interest;
    }

    public ScheduleEvent getEvent() {
        return event;
    }

    public void setEvent(ScheduleEvent event) {
        this.event = event;
    }

    public LoanValues getLoan() {
        return loan;
    }

    public void setLoan(LoanValues loan) {
        this.loan = loan;
    }

    public InstallmentValues getInstallment() {
        return installment;
    }

    public void setInstallment(InstallmentValues installment) {
        this.installment = installment;
    }

    public PrincipalValues getPrincipal() {
        return principal;
    }

    public void setPrincipal(PrincipalValues principal) {
        this.principal = principal;
    }

    public InterestValues getInterest() {
        return interest;
    }

    public void setInterest(InterestValues interest) {
        this.interest = interest;
    }
}
