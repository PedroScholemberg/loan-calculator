package com.loancalculator.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class PaymentScheduleGenerator {

    public PaymentSchedule generate(Loan loan) {
        LocalDate startDate = loan.getStartDate();
        LocalDate endDate = loan.getEndDate();
        LocalDate firstPaymentDate = loan.getFirstPaymentDate();
        int paymentDay = firstPaymentDate.getDayOfMonth();
        boolean monthEndPayments = paymentDay == firstPaymentDate.lengthOfMonth();
        Map<LocalDate, ScheduleEvent> events = new TreeMap<>();

        eventOn(events, startDate).setLoanStart(true);

        YearMonth month = YearMonth.from(startDate);
        YearMonth lastMonth = YearMonth.from(endDate);
        while (!month.isAfter(lastMonth)) {
            LocalDate monthEnd = month.atEndOfMonth();
            if (!monthEnd.isAfter(endDate)) {
                eventOn(events, monthEnd).setMonthEnd(true);
            }

            LocalDate paymentDate = monthEndPayments
                    ? monthEnd
                    : month.atDay(Math.min(paymentDay, month.lengthOfMonth()));
            if (!paymentDate.isBefore(firstPaymentDate)
                    && !paymentDate.isBefore(startDate)
                    && !paymentDate.isAfter(endDate)) {
                LocalDate adjustedDate = adjustWeekend(paymentDate);
                if (!adjustedDate.isAfter(endDate)) {
                    eventOn(events, adjustedDate).setPayment(true);
                }
            }

            if (month.equals(lastMonth)) {
                break;
            }
            month = month.plusMonths(1);
        }

        eventOn(events, endDate).setPayment(true);

        int installmentNumber = 1;
        for (ScheduleEvent event : events.values()) {
            if (event.isPayment()) {
                event.setInstallmentNumber(installmentNumber++);
            }
        }

        return new PaymentSchedule(new ArrayList<>(events.values()));
    }

    private LocalDate adjustWeekend(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY -> date.plusDays(2);
            case SUNDAY -> date.plusDays(1);
            default -> date;
        };
    }

    private ScheduleEvent eventOn(Map<LocalDate, ScheduleEvent> events, LocalDate date) {
        return events.computeIfAbsent(date,
                key -> new ScheduleEvent(key, false, false, false, null));
    }
}
