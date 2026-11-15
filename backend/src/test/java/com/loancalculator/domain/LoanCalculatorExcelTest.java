package com.loancalculator.domain;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sheet0!A6:J246 from the original TOTVS workbook, including saved formula results.
 * The fixture records its source SHA-256 and retains numeric XML text, not recomputed values.
 * Excel's money format (numFmtId 165) displays two decimals; HALF_UP models that presentation.
 * The spreadsheet leaves a roughly -1.5E-10 final principal residue; it displays as zero.
 * Internal production invariants below are checked without cent rounding or a tolerance.
 *
 * PDF requirements: base 360, monthly installments, all month-end dates, mandatory final payment.
 * Known limitation: production has no holiday calendar. This test uses exact Excel dates
 * and installment identifiers so calendar differences cannot masquerade as formula failures.
 */
class LoanCalculatorExcelTest {
    private static final List<String> MONEY_FIELDS = List.of("loanAmount", "outstandingBalance",
            "totalPayment", "amortization", "remainingPrincipal", "interestProvision",
            "accumulatedInterest", "paidInterest");
    private static final List<ReferenceRow> REFERENCE = readReference();
    private static final Loan LOAN = new Loan(LocalDate.of(2024, 1, 1), LocalDate.of(2034, 1, 1),
            LocalDate.of(2024, 2, 15), new BigDecimal("140000.00"), new BigDecimal("0.07"));

    private static PaymentSchedule referenceSchedule() {
        return new PaymentSchedule(REFERENCE.stream().map(row -> new ScheduleEvent(row.date(),
                row.date().equals(LOAN.getStartDate()), row.date().getDayOfMonth() == row.date().lengthOfMonth(),
                row.installmentNumber() != null, row.installmentNumber())).toList());
    }

    @Test
    void referenceContainsEverySpreadsheetRowAndInstallment() {
        assertEquals(241, REFERENCE.size(), "Sheet0 rows 6 through 246 inclusive");
        assertEquals(LOAN.getStartDate(), REFERENCE.get(0).date());
        assertEquals(LOAN.getEndDate(), REFERENCE.get(REFERENCE.size() - 1).date());
        assertEquals(120, referenceSchedule().getInstallmentCount(), "Excel G2");
        int number = 0;
        for (int i = 0; i < REFERENCE.size(); i++) {
            ReferenceRow row = REFERENCE.get(i);
            assertEquals(i + 6, row.excelRow(), "No missing or duplicate source rows");
            if (i > 0) assertTrue(row.date().isAfter(REFERENCE.get(i - 1).date()), "Chronological unique dates");
            if (row.installmentNumber() != null) {
                assertEquals(++number + "/120", row.installment(), "Original Excel installment identifier");
            }
        }
    }

    @TestFactory
    Stream<DynamicTest> everyRowMatchesExcelAtDisplayedPrecision() {
        LoanSimulationResult actual = new LoanCalculator().calculate(LOAN, referenceSchedule());
        assertEquals(REFERENCE.size(), actual.getRows().size());
        assertEquals(referenceSchedule().getInstallmentCount(), actual.getInstallmentCount());
        return IntStream.range(0, REFERENCE.size()).mapToObj(i -> {
            ReferenceRow expected = REFERENCE.get(i);
            LoanCalculationRow row = actual.getRows().get(i);
            String context = "Row " + expected.excelRow() + " — " + expected.date();
            return DynamicTest.dynamicTest(context, () -> {
                List<Executable> checks = new ArrayList<>();
                checks.add(() -> assertEquals(expected.date(), row.getEvent().getDate(), context + " Field: date"));
                checks.add(() -> assertEquals(expected.installmentNumber(), row.getEvent().getInstallmentNumber(),
                        context + " Field: installmentNumber Expected: " + expected.installmentNumber()
                                + " Actual: " + row.getEvent().getInstallmentNumber()));
                List<BigDecimal> values = monetaryValues(row);
                for (int field = 0; field < MONEY_FIELDS.size(); field++) {
                    int index = field;
                    checks.add(() -> {
                        BigDecimal wanted = expected.money().get(index).setScale(2, RoundingMode.HALF_UP);
                        BigDecimal calculated = values.get(index).setScale(2, RoundingMode.HALF_UP);
                        assertEquals(wanted, calculated, context + "\nField: " + MONEY_FIELDS.get(index)
                                + "\nExpected: " + wanted + " (raw " + expected.money().get(index) + ")"
                                + "\nActual: " + calculated + " (raw " + values.get(index) + ")");
                    });
                }
                assertAll(context, checks);
            });
        });
    }

    @Test
    void unroundedFinancialInvariantsHoldForEveryReferenceEvent() {
        List<LoanCalculationRow> rows = new LoanCalculator().calculate(LOAN, referenceSchedule()).getRows();
        List<Executable> checks = new ArrayList<>();
        BigDecimal previous = LOAN.getPrincipal();
        BigDecimal totalAmortization = BigDecimal.ZERO;
        for (int i = 0; i < rows.size(); i++) {
            LoanCalculationRow row = rows.get(i);
            String context = "Row " + REFERENCE.get(i).excelRow() + " — " + row.getEvent().getDate();
            BigDecimal principal = row.getPrincipal().getRemainingBalance();
            BigDecimal amortization = row.getPrincipal().getAmortization();
            BigDecimal accumulated = row.getInterest().getAccumulated();
            BigDecimal before = previous;
            checks.add(() -> exact(principal.add(accumulated), row.getLoan().getOutstandingBalance(),
                    context + " Field: outstandingBalance"));
            checks.add(() -> exact(amortization.add(row.getInterest().getPaid()), row.getInstallment().getTotal(),
                    context + " Field: totalPayment"));
            checks.add(() -> exact(before.subtract(amortization), principal, context + " Field: remainingPrincipal"));
            if (row.getEvent().isPayment()) {
                checks.add(() -> assertTrue(principal.compareTo(before) < 0, context + " Principal decreases on payment"));
                checks.add(() -> exact(BigDecimal.ZERO, accumulated, context + " Field: accumulatedInterest"));
            } else {
                checks.add(() -> exact(before, principal, context + " Non-payment principal unchanged"));
                checks.add(() -> exact(BigDecimal.ZERO, amortization, context + " Non-payment amortization"));
            }
            previous = principal;
            totalAmortization = totalAmortization.add(amortization);
        }
        BigDecimal total = totalAmortization;
        checks.add(() -> exact(BigDecimal.ZERO, rows.get(rows.size() - 1).getPrincipal().getRemainingBalance(),
                "Final principal"));
        checks.add(() -> exact(LOAN.getPrincipal(), total, "Total amortization"));
        assertAll("Unrounded invariants", checks);
    }

    private static void exact(BigDecimal expected, BigDecimal actual, String context) {
        assertEquals(0, expected.compareTo(actual), context + " Expected: " + expected + " Actual: " + actual);
    }

    private static List<BigDecimal> monetaryValues(LoanCalculationRow row) {
        return List.of(row.getLoan().getLoanAmount(), row.getLoan().getOutstandingBalance(),
                row.getInstallment().getTotal(), row.getPrincipal().getAmortization(),
                row.getPrincipal().getRemainingBalance(), row.getInterest().getProvision(),
                row.getInterest().getAccumulated(), row.getInterest().getPaid());
    }

    private static List<ReferenceRow> readReference() {
        try (var reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                LoanCalculatorExcelTest.class.getResourceAsStream("/loan-calculation-reference.csv")),
                StandardCharsets.UTF_8))) {
            return reader.lines().filter(line -> !line.startsWith("#"))
                    .skip(1).map(line -> {
                        String[] c = line.split(",", -1);
                        if (c.length != 11) throw new IllegalArgumentException("Invalid fixture row: " + line);
                        List<BigDecimal> money = Stream.of(c[2], c[3], c[5], c[6], c[7], c[8], c[9], c[10])
                                .map(BigDecimal::new).toList();
                        return new ReferenceRow(Integer.parseInt(c[0]), LocalDate.parse(c[1]), c[4], money);
                    }).toList();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot read original Excel reference fixture", ex);
        }
    }

    private record ReferenceRow(int excelRow, LocalDate date, String installment, List<BigDecimal> money) {
        Integer installmentNumber() {
            return installment.isEmpty() ? null : Integer.valueOf(installment.split("/")[0]);
        }
    }
}
