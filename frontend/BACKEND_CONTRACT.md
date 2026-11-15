# Backend API contract — Loan Amortization Calculator

The frontend calls `POST /api/v1/loans/amortization-schedule` and performs no financial calculations.

## Configuration and request

`VITE_API_BASE_URL` defaults to `http://localhost:8080`; trailing slashes are stripped.
The client sends `Content-Type: application/json` and `Accept: application/json`.
Development CORS allows `http://localhost:5173` and `http://localhost:4173`, POST/OPTIONS and Content-Type/Accept. Backend allowed origins are configurable.

```ts
interface AmortizationScheduleRequest {
  startDate: string         // YYYY-MM-DD
  endDate: string
  firstPaymentDate: string
  principal: number        // BRL
  annualInterestRate: number // percentage points: 7 means 7% annually
}
```

All fields are required. Both frontend and backend enforce
`startDate < firstPaymentDate < endDate`, positive principal with at most two decimal places,
and a nonnegative annual rate. Required and invalid calendar dates are rejected.
The Calculate button is disabled while any input is invalid or a request is pending.
The first-payment upper-bound message is `O primeiro pagamento deve ser anterior à data final.`
Inputs accept Brazilian numeric notation; dates are sent as ISO calendar dates without time zones.
The endpoint and request parameters are unchanged.

## Response (200 OK)

```ts
type ScheduleEventType = 'LOAN_START' | 'MONTH_END' | 'PAYMENT' | 'LOAN_END'

interface ScheduleEntry {
  date: string
  eventType: ScheduleEventType
  installmentNumber: number | null
  loanAmount: number
  outstandingBalance: number
  totalPayment: number
  amortization: number
  remainingPrincipal: number
  interestProvision: number
  accumulatedInterest: number
  paidInterest: number
}

interface ScheduleSummary {
  installmentCount: number
  totalInterest: number
  totalAmortization: number
  totalPayment: number
}

interface AmortizationScheduleResponse {
  schedule: ScheduleEntry[]
  summary: ScheduleSummary
}
```

| Group | Column in display order | Source and meaning |
| --- | --- | --- |
| General | Data Competência | `date`: financial event date |
| Empréstimo | Valor de Empréstimo | `loanAmount`: original principal, displayed only on the first event (zero thereafter in the API) |
| Empréstimo | Saldo Devedor | `outstandingBalance`: remaining principal plus unpaid accumulated interest |
| Parcela | Consolidada | `installmentNumber/summary.installmentCount`; blank when number is null |
| Parcela | Total | `totalPayment`: principal amortization plus interest paid |
| Principal | Amortização | `amortization`: principal repaid at this event |
| Principal | Saldo | `remainingPrincipal`: remaining principal after this event |
| Juros | Provisão | `interestProvision`: interest provision for this event |
| Juros | Acumulado | `accumulatedInterest`: interest accrued and still unpaid after this event |
| Juros | Pago | `paidInterest`: interest paid at this event |

All entry fields are required. Only `installmentNumber` may be null, for non-payment events.
A final payment retains its installment number even when `eventType` is `LOAN_END`.
Coincident financial events share a single row; event type precedence is LOAN_END,
LOAN_START, PAYMENT, MONTH_END. Event type supplies row styling, not an extra column.
Installment numbering comes from ScheduleEvent and the count from LoanSimulationResult.
The domain stores no formatted installment identifiers.

The legacy `openingBalance`, `interest`, `payment`, and `closingBalance` fields were removed.
All money is serialized as JSON numbers, rounded HALF_EVEN to two decimal places only at the API boundary.
Mapping copies domain values without recalculating financial formulas. Internal precision is preserved.
Summary totals sum unrounded domain paid interest, amortization and payments before rounding;
therefore sums of individually rounded rows can differ by cents from summary totals.
The three existing totals are preserved; `installmentCount` is the only new summary field.

The frontend renders rows in the chronological order supplied by the backend, without sorting,
filtering or financial calculations. Dates display as DD/MM/YYYY and money uses
`Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`.
The semantic table has grouped headers and a caption inside a keyboard-focusable horizontal scroll region.
An empty schedule displays `Nenhum evento foi retornado para os parâmetros informados.`
Missing schedule or summary is treated as an unexpected-response error.

Illustrative first event (not a full calculation):

```json
{
  "date": "2024-01-01",
  "eventType": "LOAN_START",
  "installmentNumber": null,
  "loanAmount": 140000.00,
  "outstandingBalance": 140000.00,
  "totalPayment": 0.00,
  "amortization": 0.00,
  "remainingPrincipal": 140000.00,
  "interestProvision": 0.00,
  "accumulatedInterest": 0.00,
  "paidInterest": 0.00
}
```

## Errors

The backend uses RFC 9457 Problem Details with optional field errors:

```ts
interface ApiProblem {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  errors?: { field: string; message: string }[]
}
```

400 indicates malformed or invalid input; 422 indicates inputs that cannot be calculated.
For these statuses the client displays the Portuguese `detail` when present, with its existing
friendly fallback otherwise. 500 and other failures display a generic message without server internals.
Network failures have a connection-error message. Errors retain the retry button.
Field-level server errors are not mapped into form fields. No authentication or persistence is used.
