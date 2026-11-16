# Business Rules

## Inputs and validation

The API accepts `POST /api/v1/loans/amortization-schedule`. Its authoritative constraints are in [AmortizationScheduleRequest.java](../backend/src/main/java/com/loancalculator/dto/AmortizationScheduleRequest.java).

| Field | Implemented constraint |
| --- | --- |
| `startDate` | Required calendar date. |
| `endDate` | Required and strictly after `startDate`. |
| `firstPaymentDate` | Required and strictly between start and end. |
| `principal` | Required, greater than zero, with at most two decimal places after trailing zeros are removed. |
| `annualInterestRate` | Required and nonnegative; expressed in percentage points, so `7` becomes a domain rate of `0.07`. |

Dates are represented by `LocalDate`; the documented JSON format is `YYYY-MM-DD`, without time or time zone. No explicit maximum principal, annual rate, or term length is declared in the request DTO. Validating those constraints does not guarantee that extreme inputs can be calculated within numerical or resource limits.

The frontend additionally uses date-input bounds of 1900-01-01 through 9999-12-31 and four-digit date parsing; these are not backend DTO constraints. Its principal parser counts typed fractional digits, so `100,000` is rejected even though the backend accepts numeric `100.000` after stripping trailing zeros. Zero interest is accepted. Principal dots may denote thousands groups; rate dots denote decimal fractions. Comma decimals are supported in both fields. Values are sent as JavaScript numbers, not localized strings.

## Schedule generation

[PaymentScheduleGenerator.java](../backend/src/main/java/com/loancalculator/domain/PaymentScheduleGenerator.java) creates one event per date in a sorted map:

1. Add the loan start date.
2. Add every calendar month-end from the start month through the end date, including month-ends before the first payment.
3. Generate monthly payment candidates using the original first-payment day. If that date is the last day of its month, all candidates use month-end. Otherwise clamp that day to the current month's length without changing the original anchor.
4. Consider candidates on or after both start and first-payment dates, and on or before end. Move Saturday candidates to Monday and Sunday candidates to Monday. Exclude adjusted dates after the end date.
5. Always add a payment on the exact end date, even on a weekend. This final payment is not moved.
6. Merge coincident start, month-end, and payment flags into one event. Assign payment numbers sequentially from one in chronological order; nonpayment numbers are null.

A moved payment may cross into the following month; subsequent candidates retain the original anchor. A monthly payment coinciding with the final date is one installment, not two. If all monthly candidates move past the end, the mandatory final payment still remains. Month-end events themselves are never weekend-adjusted.

Public holidays are not checked. A weekday holiday remains a payment date, and Monday is accepted even if it is a holiday. The final-date exception also applies independently of holidays.

## Financial formulas

[LoanCalculator.java](../backend/src/main/java/com/loancalculator/domain/LoanCalculator.java) uses actual elapsed calendar days divided by a fixed 360-day base. This is not a synthetic 30-day-per-month calculation.

Let `P` be original principal, `r = annualInterestRate / 100`, and `N` the number of generated payment events, including the final payment. Regular principal amortization is `A = P / N`. Total installments are not fixed: their interest component depends on elapsed time and outstanding balances.

At the initial event, remaining principal and outstanding balance are `P`; disbursement (`loanAmount`) is `P`; provision, accumulated interest, paid interest, amortization, and payment are zero. Later disbursements are zero.

For each later event `i`, let `d_i` be calendar days since the preceding event, `B_(i-1)` remaining principal, and `U_(i-1)` unpaid accumulated interest:

```text
g_i = (1 + r)^(d_i / 360) - 1
I_i = g_i * (B_(i-1) + U_(i-1))
D_i = U_(i-1) + I_i

a_i = 0                         on nonpayment events
      A                         on payments before the last
      B_(i-1)                   on the last payment

j_i = D_i on payment events; otherwise 0
U_i = D_i - j_i
B_i = B_(i-1) - a_i
O_i = B_i + U_i
T_i = a_i + j_i on payment events; otherwise 0
```

`I_i` is provision, `j_i` is paid interest, `O_i` is outstanding balance, and `T_i` is total payment. Unpaid interest participates in the next interval's interest-bearing balance, including across month-end events. Payments settle all accumulated interest. Zero rate or zero elapsed days produces zero growth.

The implementation evaluates growth with `StrictMath.expm1(StrictMath.log1p(r.doubleValue()) * (days / 360.0))`, then converts it using `BigDecimal.valueOf`. This avoids subtracting nearly equal floating-point values for small growth but still uses double precision for the fractional power.

## Precision and outputs

Principal division and provision multiplication use a 34-significant-digit `MathContext` with `HALF_EVEN`. Other balance additions and subtractions use `BigDecimal` without a cent-scale rounding step. The final amortization consumes the actual remaining principal to eliminate division residue.

[LoanService.java](../backend/src/main/java/com/loancalculator/service/LoanService.java) rounds every monetary response field to two decimal places with `HALF_EVEN`. Summary totals sum original paid interest, amortization, and payment values before rounding. Therefore a sum of displayed row amounts can differ from the displayed summary; independently rounded components need not add to the rounded row total exactly.

`schedule` entries expose date, event type, installment number, original disbursement, outstanding balance, total payment, amortization, remaining principal, interest provision, accumulated interest, and paid interest. `summary` exposes installment count, total paid interest, total amortization, and total payment. API money values are JSON numbers; the frontend formats them as BRL.

An event may carry multiple domain flags, but the API exposes one type with precedence `LOAN_END`, `LOAN_START`, `PAYMENT`, then `MONTH_END`. The final row retains its payment number even though its type is `LOAN_END`.

## Invariants and exceptional outcomes

At domain precision, outstanding balance equals remaining principal plus unpaid interest; payment equals amortization plus paid interest; principal decreases by amortization; nonpayments preserve principal and pay nothing. Payments clear accumulated interest. Final amortization settles remaining principal, and total amortization equals original principal. Reference tests assert these identities without cent rounding or tolerance for their reference scenario; they do not prove every possible input.

The service rejects empty results, zero installment count, or a nonzero final outstanding balance. Arithmetic, numerical-conversion, and date exceptions from scheduling/calculation become calculation failures.

| HTTP status | Behavior |
| --- | --- |
| 200 | Calculated schedule and summary. |
| 400 | Missing/invalid fields or unreadable JSON/date/number representations; validation messages are deduplicated and sorted. |
| 422 | Calculation failure or failed settlement checks. |
| 500 | Unexpected failure; logged on the server with a generic client message. |

Problem Details messages and the UI are currently Portuguese. Cause details and stack traces are not returned by the custom handlers. Negative rates, nonpositive principal, and first payments on either boundary are unsupported through the API. There are no inputs for extra repayments, fees, variable rates, alternative amortization methods, or holiday calendars.
