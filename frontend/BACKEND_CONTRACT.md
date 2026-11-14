# Backend API contract — Loan Amortization Calculator

This document describes everything the frontend expects from the Spring Boot
backend. It reflects the code as implemented in:

- `src/features/loan-calculator/loan.types.ts` — TypeScript types (source of truth for the frontend)
- `src/features/loan-calculator/loanApi.ts` — endpoint call and error-message mapping
- `src/api/httpClient.ts` — HTTP client (`fetch`, JSON, error handling)
- `src/config/env.ts` — API base URL configuration
- `src/features/loan-calculator/loanValidation.ts` — client-side validation rules

The frontend performs **no financial calculations**. Every value in the
schedule and in the summary is displayed exactly as returned by the backend.

---

## 1. Configuration

| Item | Value |
| --- | --- |
| Environment variable | `VITE_API_BASE_URL` (build-time, read by Vite) |
| Default when unset | `http://localhost:8080` (Spring Boot default port) |
| Trailing slash | Optional; it is stripped by the frontend |
| Example file | `frontend/.env.example` (copy to `.env.local` to override) |

The full request URL is `${VITE_API_BASE_URL}/api/v1/loans/amortization-schedule`.

For production builds, set `VITE_API_BASE_URL` at build time
(for example `VITE_API_BASE_URL=https://api.example.com npm run build`).

## 2. CORS

In development the frontend runs on a different origin from the API, so the
browser sends a CORS preflight (`OPTIONS`) before every `POST`, because the
request has `Content-Type: application/json`.

The backend must:

| Requirement | Value |
| --- | --- |
| Allowed origins | `http://localhost:5173` (`npm run dev`) and `http://localhost:4173` (`npm run preview`); production origin configurable |
| Allowed methods | `POST`, `OPTIONS` |
| Allowed request headers | `Content-Type`, `Accept` |
| Credentials | Not used (no cookies, no `Authorization` header) |
| Preflight response | `200` or `204` with the headers above |

Keep the allowed origins in configuration (for example an
`application.properties` entry) rather than hard-coding them. Do not use `*`
in production.

If the API is later served from the same origin as the frontend (for example
behind the planned Nginx reverse proxy), CORS is no longer needed. In that
case `VITE_API_BASE_URL` can point at that origin.

## 3. Endpoint

```
POST /api/v1/loans/amortization-schedule
Content-Type: application/json
Accept: application/json
```

This is the only endpoint the frontend calls. It is a pure calculation: it
must not persist anything. On success it returns **`200 OK`**, not `201`.

## 4. Request

### TypeScript type

```ts
type IsoDate = string // "YYYY-MM-DD"

interface AmortizationScheduleRequest {
  startDate: IsoDate
  endDate: IsoDate
  firstPaymentDate: IsoDate
  principal: number          // BRL, e.g. 140000.00
  annualInterestRate: number // percentage points: 7.0 means 7% per year
}
```

### Fields

| Field | JSON type | Suggested Java type | Meaning (UI label) |
| --- | --- | --- | --- |
| `startDate` | string `YYYY-MM-DD` | `LocalDate` | Loan start date (*Data inicial*) |
| `endDate` | string `YYYY-MM-DD` | `LocalDate` | Loan end date (*Data final*) |
| `firstPaymentDate` | string `YYYY-MM-DD` | `LocalDate` | Date of the first payment (*Primeiro pagamento*) |
| `principal` | number | `BigDecimal` | Loan amount in BRL (*Valor do empréstimo*) |
| `annualInterestRate` | number | `BigDecimal` | Annual interest rate in percentage points (*Taxa de juros anual (%)*) |

All five fields are always sent and are never `null`.

### Validation rules

The frontend enforces these rules before sending, so a valid UI never sends a
request that breaks them. **The backend must still validate independently**
and return `400` when a rule fails, because the API can be called directly.

| # | Rule | Frontend message (pt-BR) |
| --- | --- | --- |
| V1 | All three dates are present and valid calendar dates | `Informe uma data … válida.` |
| V2 | `endDate` > `startDate` (strictly) | `A data final deve ser posterior à data inicial.` |
| V3 | `firstPaymentDate` > `startDate` (strictly) | `O primeiro pagamento deve ser posterior à data inicial.` |
| V4 | `firstPaymentDate` ≤ `endDate` (equal is allowed) | `O primeiro pagamento não pode ultrapassar a data final.` |
| V5 | `principal` > 0 | `O valor do empréstimo deve ser maior que zero.` |
| V6 | `principal` has at most 2 decimal places (cents) | `Informe o valor com no máximo 2 casas decimais.` |
| V7 | `annualInterestRate` ≥ 0 (zero is allowed) | `A taxa de juros não pode ser negativa.` |

V6 is a frontend assumption (BRL has cents). See [Open questions](#10-assumptions-and-open-questions).
No upper limits are defined for `principal`, `annualInterestRate` or the
loan period.

## 5. Response (`200 OK`)

### TypeScript types

```ts
type ScheduleEventType = 'LOAN_START' | 'MONTH_END' | 'PAYMENT' | 'LOAN_END'

interface ScheduleEntry {
  date: IsoDate
  eventType: ScheduleEventType
  openingBalance: number
  interest: number
  amortization: number
  payment: number
  closingBalance: number
}

interface ScheduleSummary {
  totalInterest: number
  totalAmortization: number
  totalPayment: number
}

interface AmortizationScheduleResponse {
  schedule: ScheduleEntry[]
  summary: ScheduleSummary
}
```

### `schedule[]` entry fields

The table shows the columns in this order:

| Column (UI) | Field | JSON type | Suggested Java type | Meaning |
| --- | --- | --- | --- | --- |
| Data | `date` | string `YYYY-MM-DD` | `LocalDate` | Date of the event |
| Evento | `eventType` | string enum | `enum` | Type of event (see below) |
| Saldo inicial | `openingBalance` | number | `BigDecimal` | Outstanding balance before the event |
| Juros | `interest` | number | `BigDecimal` | Interest attributed to the event |
| Amortização | `amortization` | number | `BigDecimal` | Principal repaid in the event |
| Pagamento | `payment` | number | `BigDecimal` | Amount paid in the event |
| Saldo final | `closingBalance` | number | `BigDecimal` | Outstanding balance after the event |

All fields are required in every entry and must not be `null`. Use `0` when a
value does not apply to an event.

### `eventType` enum

| Value | UI label (pt-BR) | Visual treatment |
| --- | --- | --- |
| `LOAN_START` | Início do empréstimo | Green badge |
| `MONTH_END` | Fechamento mensal | Muted row, grey badge |
| `PAYMENT` | Pagamento | Blue badge, accent bar |
| `LOAN_END` | Encerramento | Dark badge |

If the backend sends an unknown value, the frontend shows the raw code
instead of failing. New values should still be added to this contract and to
`loan.types.ts`.

### `summary` fields

| UI label | Field | JSON type | Meaning |
| --- | --- | --- | --- |
| Total de juros | `totalInterest` | number | Total interest over the loan |
| Total amortizado | `totalAmortization` | number | Total principal repaid |
| Total pago | `totalPayment` | number | Total amount paid |

The frontend **does not** sum the schedule. It shows these three numbers
as-is, so the backend is responsible for keeping them consistent with the rows.

### Ordering and content

- The frontend renders `schedule` **in the exact order received**. It does not
  sort, filter, merge or add rows. The backend decides the order, including
  how events on the same date are ordered.
- An empty `schedule` (`[]`) is valid. The UI shows "Nenhum evento foi
  retornado para os parâmetros informados." In that case `summary` must
  still be present.
- If the response has no `schedule` array or no `summary` object, the
  frontend treats it as an error ("O servidor retornou uma resposta
  inesperada").

## 6. Data representation

| Aspect | Rule |
| --- | --- |
| Dates (request and response) | ISO-8601 calendar date string `YYYY-MM-DD`, with no time or time zone. Never send timestamps, epoch numbers or `[2024, 1, 31]` arrays. Jackson's `LocalDate` default in Spring Boot is correct. |
| Date display | The frontend converts to `DD/MM/AAAA` as text (no time zone conversion). |
| Money | JSON **numbers** in BRL with no currency symbol, for example `140000.00`. Do not serialize as strings. Return values already rounded to 2 decimal places. |
| Money display | The frontend formats with `Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })`, for example `R$ 140.000,00`. |
| Percentage | `annualInterestRate` is in **percentage points**: `7.0` = 7% per year, `0.5` = 0.5% per year (not `0.07`). |
| Percentage display | The frontend shows the rate input as `7,00` with a `%` adornment. |
| Number input parsing | Users can type `140.000,00`, `140000,00` or `140000`; the frontend always sends a plain JSON number (`140000`). The rate input treats `.` as a decimal separator (`7.5` → `7.5`). |
| Decimal precision | JSON numbers are parsed as IEEE-754 doubles in JavaScript. Deserialize them into `BigDecimal` on the backend (Jackson does this for `BigDecimal` fields). |
| Trailing zeros | `140000` and `140000.00` are equivalent JSON; the frontend may send either. |

## 7. Errors

### Status codes

| Status | When | Message shown to the user |
| --- | --- | --- |
| `200` | Success | — (renders summary and schedule) |
| `400` | Malformed JSON, missing or `null` fields, wrong types, or validation rules V1–V7 failed | `Parâmetros inválidos: {detail}` when `detail` is present, otherwise `Os parâmetros informados são inválidos. Revise os campos e tente novamente.` |
| `422` | Input is well-formed but cannot be calculated under the financial rules | `Não foi possível calcular a amortização: {detail}` when `detail` is present, otherwise `Os parâmetros informados não são compatíveis com as regras do empréstimo. Revise os valores e tente novamente.` |
| `500` | Unexpected server error | `O servidor encontrou um erro ao processar a simulação. Tente novamente mais tarde.` (the body is never shown) |
| Other non-2xx | — | Same generic message as `500` |
| No response | Server down, CORS rejection, network failure | `Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.` |

Every error state shows a **Tentar novamente** button that resends the same
request.

### Error body

Use RFC 9457 Problem Details, which Spring's `ProblemDetail` produces natively
(`Content-Type: application/problem+json` is fine):

```ts
interface ApiProblem {
  type?: string
  title?: string
  status?: number
  detail?: string                             // shown to the user for 400/422
  instance?: string
  errors?: { field: string; message: string }[] // optional, per-field details
}
```

- **`detail` is displayed verbatim to the end user for `400` and `422`.** It
  must be a short, user-friendly sentence **in Brazilian Portuguese**. It must
  not contain stack traces, class names or SQL. Omit it if no such message
  is available.
- For `500`, the frontend ignores the body, so it can hold anything the
  backend needs, but it should still not leak internals.
- `errors[].field` should use the request field names (`startDate`,
  `principal`, …). The frontend does not currently map `errors[]` to
  individual inputs; see [Open questions](#10-assumptions-and-open-questions).
- An empty or non-JSON error body is tolerated; the generic message is used.

## 8. Example

> The numbers below are **illustrative only**. They come from the frontend
> specification and are not expected calculation results.

### Request

```bash
curl -X POST http://localhost:8080/api/v1/loans/amortization-schedule \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
    "startDate": "2024-01-01",
    "endDate": "2034-01-01",
    "firstPaymentDate": "2024-02-15",
    "principal": 140000.00,
    "annualInterestRate": 7.0
  }'
```

### Response — `200 OK`

```json
{
  "schedule": [
    {
      "date": "2024-01-31",
      "eventType": "MONTH_END",
      "openingBalance": 140000.00,
      "interest": 0.00,
      "amortization": 0.00,
      "payment": 0.00,
      "closingBalance": 140000.00
    }
  ],
  "summary": {
    "totalInterest": 0.00,
    "totalAmortization": 140000.00,
    "totalPayment": 0.00
  }
}
```

### Response — `400 Bad Request`

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "A data final deve ser posterior à data inicial.",
  "instance": "/api/v1/loans/amortization-schedule",
  "errors": [
    { "field": "endDate", "message": "A data final deve ser posterior à data inicial." }
  ]
}
```

### Response — `422 Unprocessable Entity`

```json
{
  "type": "about:blank",
  "title": "Unprocessable Entity",
  "status": 422,
  "detail": "Os parâmetros não permitem gerar um cronograma de pagamentos.",
  "instance": "/api/v1/loans/amortization-schedule"
}
```

## 9. Backend checklist

The backend must provide:

- [ ] `POST /api/v1/loans/amortization-schedule`, accepting and returning `application/json`
- [ ] Request DTO with `startDate`, `endDate`, `firstPaymentDate` (`LocalDate`), and `principal`, `annualInterestRate` (`BigDecimal`)
- [ ] Server-side validation of rules V1–V7, returning `400` with a Problem Details body
- [ ] `422` with a pt-BR `detail` for inputs that are valid but cannot be calculated
- [ ] `500` without internal details for unexpected failures
- [ ] Response with a `schedule` array, where each entry has `date`, `eventType`, `openingBalance`, `interest`, `amortization`, `payment`, `closingBalance`
- [ ] `eventType` limited to `LOAN_START`, `MONTH_END`, `PAYMENT`, `LOAN_END`
- [ ] `summary` with `totalInterest`, `totalAmortization`, `totalPayment`, computed by the backend
- [ ] Schedule already in display order
- [ ] Monetary values as JSON numbers rounded to 2 decimal places; dates as `YYYY-MM-DD` strings
- [ ] `annualInterestRate` interpreted as percentage points (`7.0` = 7% per year)
- [ ] CORS for `http://localhost:5173` and `http://localhost:4173` (configurable), methods `POST` and `OPTIONS`, header `Content-Type`
- [ ] No persistence and no authentication

## 10. Assumptions and open questions

The frontend specification leaves these points undefined. The frontend does
not depend on how they are resolved, but the backend developer must decide
them.

1. **Calculation method.** The interest formula, day-count convention
   (for example 30/360, actual/365 or actual/360), compounding, and how the
   annual rate converts to a period rate are not specified. They are entirely
   up to the backend.
2. **Payment frequency and amount.** The spec only provides
   `firstPaymentDate`. It does not say how often payments recur (assumed
   monthly?), on which day, how each payment is sized (constant installment,
   constant amortization, …), or what happens when a day does not exist in a
   month (for example the 31st).
3. **Which events appear and when.** It is not defined whether there is
   exactly one `LOAN_START` and one `LOAN_END`, whether `MONTH_END` falls on the
   last calendar day of each month, or how a `MONTH_END` and a `PAYMENT` on the
   same date are ordered. The frontend renders whatever is returned, in order.
4. **Meaning of the columns per event.** For example, whether `interest` on a
   `MONTH_END` row is interest accrued in the month or interest due. The spec's
   example shows a `MONTH_END` row with zero interest, but the values are
   marked illustrative.
5. **Rounding.** The rounding mode (`HALF_UP` or `HALF_EVEN`) and when it is
   applied (per row or only for display) are not specified. The frontend
   assumes values arrive already rounded to 2 decimal places and does not
   round again beyond 2-decimal currency formatting.
6. **Last-payment adjustment.** How rounding residue is absorbed so that the
   final `closingBalance` is exactly `0` is not specified.
7. **Example summary values.** The spec's example has
   `totalAmortization = 140000.00` with `totalPayment = 0.00`. This is treated
   as illustrative and must not be used as a reference result.
8. **Boundary between `400` and `422`.** The frontend treats both as "fix the
   inputs". Assumed split: `400` for structural or field-rule violations
   (V1–V7), `422` for combinations that pass those rules but cannot produce
   a schedule.
9. **`firstPaymentDate == endDate`.** Allowed by the spec ("não pode
   ultrapassar a data final"). The backend must handle a single-payment loan.
10. **Principal precision (V6).** The frontend limits `principal` to 2 decimal
    places; the spec does not state this. The interest rate accepts any number
    of decimal places.
11. **Upper limits.** There are no maximums for amount, rate or period. Very
    long periods produce large schedules, all of which the frontend renders.
    The backend may reject them with `422` if needed.
12. **Field-level server errors.** `errors[]` is optional and not yet mapped
    to individual inputs. If the backend provides it consistently, the UI
    could show those messages next to each field in a later iteration.
13. **Production origin.** The production frontend origin and API URL are not
    known yet (Docker/Nginx is planned). These decide the CORS allow-list and
    the value of `VITE_API_BASE_URL` at build time.
