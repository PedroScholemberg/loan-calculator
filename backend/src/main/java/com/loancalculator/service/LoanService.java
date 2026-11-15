package com.loancalculator.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;

import com.loancalculator.domain.Loan;
import com.loancalculator.domain.LoanCalculationRow;
import com.loancalculator.domain.LoanCalculator;
import com.loancalculator.domain.LoanSimulationResult;
import com.loancalculator.domain.PaymentSchedule;
import com.loancalculator.domain.PaymentScheduleGenerator;
import com.loancalculator.domain.ScheduleEvent;
import com.loancalculator.dto.AmortizationScheduleRequest;
import com.loancalculator.dto.AmortizationScheduleResponse;
import com.loancalculator.dto.ScheduleEntry;
import com.loancalculator.dto.ScheduleEventType;
import com.loancalculator.dto.ScheduleSummary;
import com.loancalculator.exception.LoanCalculationException;
import org.springframework.stereotype.Service;

@Service
public class LoanService {
    private final PaymentScheduleGenerator scheduleGenerator;
    private final LoanCalculator calculator;

    public LoanService(PaymentScheduleGenerator scheduleGenerator, LoanCalculator calculator) {
        this.scheduleGenerator = scheduleGenerator;
        this.calculator = calculator;
    }

    public AmortizationScheduleResponse calculate(AmortizationScheduleRequest request) {
        Loan loan = new Loan(request.startDate(), request.endDate(), request.firstPaymentDate(),
                request.principal(), request.annualInterestRate().movePointLeft(2));
        LoanSimulationResult result;
        try {
            PaymentSchedule schedule = scheduleGenerator.generate(loan);
            result = calculator.calculate(loan, schedule);
        } catch (ArithmeticException | NumberFormatException | DateTimeException ex) {
            throw new LoanCalculationException(ex);
        }
        if (result.getInstallmentCount() == 0 || result.getRows().isEmpty()
                || result.getRows().get(result.getRows().size() - 1)
                        .getLoan().getOutstandingBalance().signum() != 0) {
            throw new LoanCalculationException();
        }
        return toResponse(loan, result);
    }

    private AmortizationScheduleResponse toResponse(Loan loan, LoanSimulationResult result) {
        List<ScheduleEntry> entries = new ArrayList<>();
        BigDecimal totalInterest = BigDecimal.ZERO;
        BigDecimal totalAmortization = BigDecimal.ZERO;
        BigDecimal totalPayment = BigDecimal.ZERO;
        for (LoanCalculationRow row : result.getRows()) {
            entries.add(new ScheduleEntry(row.getEvent().getDate(), eventType(loan, row.getEvent()),
                    row.getEvent().getInstallmentNumber(), money(row.getLoan().getLoanAmount()),
                    money(row.getLoan().getOutstandingBalance()), money(row.getInstallment().getTotal()),
                    money(row.getPrincipal().getAmortization()), money(row.getPrincipal().getRemainingBalance()),
                    money(row.getInterest().getProvision()), money(row.getInterest().getAccumulated()),
                    money(row.getInterest().getPaid())));
            totalInterest = totalInterest.add(row.getInterest().getPaid());
            totalAmortization = totalAmortization.add(row.getPrincipal().getAmortization());
            totalPayment = totalPayment.add(row.getInstallment().getTotal());
        }
        // Sum the original values before applying presentation rounding.
        return new AmortizationScheduleResponse(entries, new ScheduleSummary(
                result.getInstallmentCount(), money(totalInterest), money(totalAmortization), money(totalPayment)));
    }

    private ScheduleEventType eventType(Loan loan, ScheduleEvent event) {
        if (event.getDate().equals(loan.getEndDate())) {
            return ScheduleEventType.LOAN_END;
        }
        if (event.isLoanStart()) {
            return ScheduleEventType.LOAN_START;
        }
        return event.isPayment() ? ScheduleEventType.PAYMENT : ScheduleEventType.MONTH_END;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }
}
