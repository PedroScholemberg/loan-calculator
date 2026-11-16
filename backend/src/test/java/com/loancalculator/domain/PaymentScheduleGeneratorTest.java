package com.loancalculator.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Known limitation: holiday adjustments are not implemented; only weekends are non-business days.
 * Financial reference tests use the exact Excel dates to isolate formulas from calendar differences.
 * These tests verify production scheduling independently, not equality to the Excel calendar.
 */
class PaymentScheduleGeneratorTest {
    private final PaymentScheduleGenerator generator = new PaymentScheduleGenerator();

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "2024-06-15, 2024-06-17", // Saturday
            "2024-09-15, 2024-09-16", // Sunday
            "2024-07-15, 2024-07-15", // Monday
            "2024-11-15, 2024-11-15"  // Friday; holidays intentionally ignored
    })
    void adjustsOnlyWeekendMonthlyPayments(LocalDate original, LocalDate expected) {
        PaymentSchedule schedule = schedule(original.withDayOfMonth(1), original,
                original.withDayOfMonth(original.lengthOfMonth()));
        assertEquals(List.of(expected, original.withDayOfMonth(original.lengthOfMonth())), paymentDates(schedule));
        assertEquals(2, schedule.getInstallmentCount());
    }

    @Test
    void monthlyPaymentsKeepOriginalAnchorAfterWeekendAdjustments() {
        PaymentSchedule schedule = schedule("2024-06-01", "2024-06-15", "2024-11-20");
        assertEquals(List.of(LocalDate.of(2024, 6, 17), LocalDate.of(2024, 7, 15),
                LocalDate.of(2024, 8, 15), LocalDate.of(2024, 9, 16), LocalDate.of(2024, 10, 15),
                LocalDate.of(2024, 11, 15), LocalDate.of(2024, 11, 20)), paymentDates(schedule));
    }

    @Test
    void monthEndAnchorAndClosingsSurvivePaymentMovesAcrossMonths() {
        PaymentSchedule schedule = schedule("2024-05-01", "2024-05-31", "2024-09-15");
        assertEquals(List.of(LocalDate.of(2024, 5, 31), LocalDate.of(2024, 7, 1),
                LocalDate.of(2024, 7, 31), LocalDate.of(2024, 9, 2), LocalDate.of(2024, 9, 15)),
                paymentDates(schedule));
        assertEquals(List.of(LocalDate.of(2024, 5, 31), LocalDate.of(2024, 6, 30),
                LocalDate.of(2024, 7, 31), LocalDate.of(2024, 8, 31)),
                schedule.getEvents().stream().filter(ScheduleEvent::isMonthEnd).map(ScheduleEvent::getDate).toList());
        assertFalse(event(schedule, "2024-06-30").isPayment());
        assertFalse(event(schedule, "2024-08-31").isPayment());
        assertTrue(event(schedule, "2024-05-31").isPayment(), "Coincident payment and closing share an event");
    }

    @Test
    void nonMonthEndAnchorIsClampedWithoutBecomingAMonthEndAnchor() {
        PaymentSchedule schedule = schedule("2024-01-01", "2024-01-30", "2024-04-10");
        assertEquals(List.of(LocalDate.of(2024, 1, 30), LocalDate.of(2024, 2, 29),
                LocalDate.of(2024, 4, 1), LocalDate.of(2024, 4, 10)), paymentDates(schedule));
        assertTrue(event(schedule, "2024-02-29").isMonthEnd());
        assertFalse(event(schedule, "2024-03-31").isPayment(), "March 30 moves to Monday April 1");
    }

    @ParameterizedTest
    @CsvSource({"2024-06-15", "2024-06-16"})
    void mandatoryFinalPaymentRemainsOnWeekend(LocalDate end) {
        PaymentSchedule schedule = schedule(LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 15), end);
        assertEquals(List.of(LocalDate.of(2024, 5, 15), end), paymentDates(schedule));
        assertEquals(end, schedule.getEvents().get(schedule.getEvents().size() - 1).getDate());
    }

    @Test
    void adjustedPaymentCoincidingWithFinalDateIsNotDuplicated() {
        PaymentSchedule schedule = schedule("2024-06-01", "2024-06-15", "2024-06-17");
        assertEquals(List.of(LocalDate.of(2024, 6, 17)), paymentDates(schedule));
        assertEquals(2, schedule.getEvents().size(), "Start and one combined final payment");
        assertEquals(1, schedule.getInstallmentCount());
        assertEquals(1, event(schedule, "2024-06-17").getInstallmentNumber());
    }

    @Test
    void paymentShiftedPastEndIsExcludedWhileFinalPaymentRemains() {
        PaymentSchedule schedule = schedule("2024-06-01", "2024-06-15", "2024-06-16");
        assertEquals(List.of(LocalDate.of(2024, 6, 16)), paymentDates(schedule));
        assertTrue(schedule.getEvents().stream().noneMatch(e -> e.getDate().isAfter(LocalDate.of(2024, 6, 16))));
        assertEquals(1, event(schedule, "2024-06-16").getInstallmentNumber());
    }

    @Test
    void fullTermHasAllClosingsUniqueOrderedDatesAndSequentialInstallments() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2034, 1, 1);
        PaymentSchedule schedule = schedule(start, LocalDate.of(2024, 2, 15), end);
        assertEquals(120, schedule.getInstallmentCount());
        assertEquals(241, schedule.getEvents().size());
        assertEquals(start, schedule.getEvents().get(0).getDate());
        assertTrue(schedule.getEvents().get(0).isLoanStart());
        assertTrue(event(schedule, end.toString()).isPayment());
        LocalDate previous = null;
        int installment = 0;
        for (ScheduleEvent event : schedule.getEvents()) {
            assertFalse(event.getDate().isAfter(end), "No event beyond loan end");
            if (previous != null) assertTrue(event.getDate().isAfter(previous), "Unique chronological dates");
            if (event.isPayment()) assertEquals(++installment, event.getInstallmentNumber(), event.getDate().toString());
            else assertNull(event.getInstallmentNumber(), event.getDate().toString());
            previous = event.getDate();
        }
        int closingCount = 0;
        for (YearMonth month = YearMonth.from(start); !month.atEndOfMonth().isAfter(end); month = month.plusMonths(1)) {
            assertTrue(event(schedule, month.atEndOfMonth().toString()).isMonthEnd(), month.toString());
            closingCount++;
        }
        assertEquals(closingCount, schedule.getEvents().stream().filter(ScheduleEvent::isMonthEnd).count());
    }

    @Test
    void holidayCalendarDifferenceIsSeparateFromFinancialValidation() {
        PaymentSchedule schedule = schedule("2024-11-01", "2024-11-15", "2024-11-30");
        assertTrue(event(schedule, "2024-11-15").isPayment());
        assertTrue(schedule.getEvents().stream().noneMatch(e -> e.getDate().equals(LocalDate.of(2024, 11, 18))),
                "Excel uses November 18; production intentionally keeps Friday November 15");
    }

    private PaymentSchedule schedule(String start, String first, String end) {
        return schedule(LocalDate.parse(start), LocalDate.parse(first), LocalDate.parse(end));
    }

    private PaymentSchedule schedule(LocalDate start, LocalDate first, LocalDate end) {
        return generator.generate(new Loan(start, end, first, new BigDecimal("140000"), new BigDecimal("0.07")));
    }

    private static List<LocalDate> paymentDates(PaymentSchedule schedule) {
        return schedule.getEvents().stream().filter(ScheduleEvent::isPayment).map(ScheduleEvent::getDate).toList();
    }

    private static ScheduleEvent event(PaymentSchedule schedule, String date) {
        return schedule.getEvents().stream().filter(e -> e.getDate().equals(LocalDate.parse(date)))
                .findFirst().orElseThrow(() -> new AssertionError("Missing event " + date));
    }
}
