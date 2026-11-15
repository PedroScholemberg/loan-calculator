package com.loancalculator.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class LoanCalculator {
    private static final MathContext MATH_CONTEXT = new MathContext(34, RoundingMode.HALF_EVEN);
    private static final double DAY_COUNT_BASE = 360.0;

    public LoanSimulationResult calculate(Loan loan, PaymentSchedule schedule) {
        int installmentCount = schedule.getInstallmentCount();
        BigDecimal regularAmortization = installmentCount == 0
                ? BigDecimal.ZERO
                : loan.getPrincipal().divide(BigDecimal.valueOf(installmentCount), MATH_CONTEXT);
        FinancialState state = new FinancialState(
                loan.getStartDate(), loan.getPrincipal(), BigDecimal.ZERO);
        List<LoanCalculationRow> rows = new ArrayList<>();
        int remainingPayments = installmentCount;

        for (ScheduleEvent event : schedule.getEvents()) {
            LoanCalculationRow row;
            if (rows.isEmpty()) {
                row = initialRow(loan, event);
            } else {
                row = calculateRow(loan, event, state, regularAmortization, remainingPayments == 1);
            }
            rows.add(row);

            state.setPreviousDate(event.getDate());
            state.setRemainingPrincipal(row.getPrincipal().getRemainingBalance());
            state.setAccumulatedInterest(row.getInterest().getAccumulated());
            if (event.isPayment()) {
                remainingPayments--;
            }
        }

        return new LoanSimulationResult(installmentCount, rows);
    }

    private LoanCalculationRow initialRow(Loan loan, ScheduleEvent event) {
        return new LoanCalculationRow(
                event,
                new LoanValues(loan.getPrincipal(), loan.getPrincipal()),
                new InstallmentValues(BigDecimal.ZERO),
                new PrincipalValues(BigDecimal.ZERO, loan.getPrincipal()),
                new InterestValues(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private LoanCalculationRow calculateRow(Loan loan, ScheduleEvent event, FinancialState state,
                                            BigDecimal regularAmortization, boolean finalPayment) {
        long days = ChronoUnit.DAYS.between(state.getPreviousDate(), event.getDate());
        BigDecimal provision = calculateInterestProvision(loan.getInterestRate(), days, state);
        BigDecimal amortization = calculateAmortization(event, state, regularAmortization, finalPayment);
        BigDecimal interestDue = state.getAccumulatedInterest().add(provision);
        BigDecimal paidInterest = event.isPayment() ? interestDue : BigDecimal.ZERO;
        BigDecimal accumulatedInterest = interestDue.subtract(paidInterest);
        BigDecimal remainingPrincipal = state.getRemainingPrincipal().subtract(amortization);
        BigDecimal outstandingBalance = remainingPrincipal.add(accumulatedInterest);
        BigDecimal totalPayment = event.isPayment()
                ? amortization.add(paidInterest) : BigDecimal.ZERO;

        return new LoanCalculationRow(
                event,
                new LoanValues(BigDecimal.ZERO, outstandingBalance),
                new InstallmentValues(totalPayment),
                new PrincipalValues(amortization, remainingPrincipal),
                new InterestValues(provision, accumulatedInterest, paidInterest));
    }

    private BigDecimal calculateInterestProvision(BigDecimal interestRate, long days, FinancialState state) {
        BigDecimal growth = fractionalGrowth(interestRate, days);
        BigDecimal interestBearingBalance = state.getRemainingPrincipal().add(state.getAccumulatedInterest());
        return growth.multiply(interestBearingBalance, MATH_CONTEXT);
    }

    private BigDecimal fractionalGrowth(BigDecimal interestRate, long days) {
        if (days == 0 || interestRate.signum() == 0) {
            return BigDecimal.ZERO;
        }
        // expm1(log1p(rate) * days / 360) computes (1 + rate)^(days / 360) - 1
        // while avoiding cancellation for small rates and short periods. Only this
        // fractional power uses double precision (about 15-16 significant digits),
        // so its last digits may differ from Excel. BigDecimal.valueOf preserves
        // the decimal representation of that approximation; monetary arithmetic
        // otherwise uses BigDecimal, without rounding intermediate values to cents.
        double growth = StrictMath.expm1(StrictMath.log1p(interestRate.doubleValue()) * (days / DAY_COUNT_BASE));
        return BigDecimal.valueOf(growth);
    }

    private BigDecimal calculateAmortization(ScheduleEvent event, FinancialState state,
                                             BigDecimal regularAmortization, boolean finalPayment) {
        if (!event.isPayment()) {
            return BigDecimal.ZERO;
        }
        // Using the entire remaining principal on the last payment clears any
        // residual from the finite-precision division of the original principal.
        return finalPayment ? state.getRemainingPrincipal() : regularAmortization;
    }
}
