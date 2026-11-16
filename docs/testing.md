# Testing

## Commands

From the repository root, run the backend suite with Java 17:

```bash
cd backend
./mvnw clean verify
```

On Windows use `.\mvnw.cmd clean verify`. Maven Wrapper is included. The lifecycle compiles sources and tests, runs the JUnit suite, and packages the executable JAR; there is no separate Failsafe integration-test configuration.

In a separate terminal, run frontend checks with Node.js 24 and npm:

```bash
cd frontend
npm ci
npm run test -- --run
npm run lint
npm run build
```

`test` already invokes `vitest run`; the additional `--run` is redundant but valid. `npm run test:watch` starts watch mode, and `npm run typecheck` checks TypeScript separately. The build also runs `tsc -b` before Vite. Lint uses Oxlint. No percentage-based coverage claim is supported by the repository's configuration or a committed coverage report.

## Backend test boundaries

Test sources are under `backend/src/test/java/com/loancalculator/`.

| Test | Scope and boundary |
| --- | --- |
| [LoanCalculatorExcelTest](../backend/src/test/java/com/loancalculator/domain/LoanCalculatorExcelTest.java) | Plain-Java calculator verification against cached spreadsheet data and exact internal accounting identities. Supplies reference events directly. |
| [PaymentScheduleGeneratorTest](../backend/src/test/java/com/loancalculator/domain/PaymentScheduleGeneratorTest.java) | Plain-Java production calendar generation; no Spring context or spreadsheet calendar substitution. |
| [LoanControllerTest](../backend/src/test/java/com/loancalculator/controller/LoanControllerTest.java) | `@WebMvcTest` and MockMvc exercise JSON binding, Bean Validation, delegation, and exception advice with a mocked service. Financial calculations do not execute in this slice. |
| [LoanCalculatorApiApplicationTests](../backend/src/test/java/com/loancalculator/LoanCalculatorApiApplicationTests.java) | `@SpringBootTest` application-context smoke test; does not send HTTP calculation requests. |
| [OpenApiDocumentationTest](../backend/src/test/java/com/loancalculator/OpenApiDocumentationTest.java) | Full application context with MockMvc verifies the generated API contract, required request fields, error schemas, Swagger UI assets, and definition URL. |

The MVC slice integrates the transport components but is not an end-to-end financial integration test. There is no dedicated service-mapping test suite or live-server HTTP suite in the repository.

## Excel reference verification

[loan-calculation-reference.csv](../backend/src/test/resources/loan-calculation-reference.csv) records cached numeric OOXML values from `Calculadora_Emprestimos.xlsx`, range `Sheet0!A6:J246`. Its comments retain the source workbook SHA-256, input parameters, and extraction description. The reference scenario starts on 2024-01-01, ends on 2034-01-01, uses first payment 2024-02-15, principal 140000, annual rate 7%, and a 360-day base.

The fixture preserves numeric text without cent rounding. The original workbook and the PDF mentioned in test comments are not present in the tracked repository. Tests neither launch Excel nor recalculate workbook formulas; the source hash identifies the stated input artifact but is not checked against a workbook file during execution.

The test first checks fixture completeness, source-row sequence, unique chronological dates, endpoints, and installment identifiers. It then constructs a `PaymentSchedule` from the fixture's dates and installment markers, invokes the real calculator, and dynamically checks each reference row. Date and installment number must match; eight monetary fields are compared after rounding both expected and actual values to two decimals with `HALF_UP`, modeling spreadsheet display precision. Assertions identify the source row, date, field, and raw values.

Using fixture dates is important: the spreadsheet calendar includes holiday shifts that the production scheduler does not implement. Formula agreement on supplied dates does not establish that a generated production schedule matches the spreadsheet calendar or its final totals. The scheduler tests explicitly show a weekday payment on 2024-11-15 where the reference uses 2024-11-18.

The spreadsheet contains a small final principal residue that rounds to zero. The production calculator instead consumes the full remaining principal on the last payment. Display-level equality accommodates the numerical differences; it is not a bit-for-bit comparison of intermediate arithmetic.

## Precision and invariants

The calculator reference suite separately checks unrounded values with `BigDecimal.compareTo`, without a numerical tolerance or cent rounding:

- Outstanding balance equals remaining principal plus accumulated unpaid interest.
- Total payment equals amortization plus paid interest.
- Remaining principal equals preceding principal minus amortization.
- Payment events decrease principal and clear accumulated interest.
- Nonpayments leave principal unchanged and amortize zero.
- Final principal is exactly zero, and total amortization equals original principal.

These assertions strengthen the reference scenario beyond visual agreement. They do not constitute property-based testing across arbitrary loans. The reference comparison's `HALF_UP` differs from the API's `HALF_EVEN`; API tie rounding, summary aggregation from unrounded values, and row/summary cent discrepancies do not have dedicated direct assertions in the current service layer tests.

## Calendar, validation, and errors

Scheduler tests cover Saturday/Sunday moves, unchanged weekdays, stable monthly anchors, month-end anchors, leap-February clamping, cross-month moves, retained month-end closings, coincident events, sequential installment numbers, final-date weekends, exclusion of shifts beyond maturity, and holiday-calendar differences. The final payment is explicitly tested on both Saturday and Sunday without adjustment.

Controller tests submit missing/null fields, nonpositive and overprecision principal, negative rates, invalid date ordering, malformed JSON, impossible dates, and invalid numeric content. Valid cases include zero/fractional rates, a cent principal, and trailing-zero principal precision. Invalid requests must not call the service; accepted values are delegated unchanged.

Mocked calculation failures exercise 422 responses, and injected unexpected exceptions exercise generic 500 responses. Assertions check Problem Details status, title, detail, instance, content type, and omission of internal markers, causes, and stack traces. These tests verify translation of failures, not the real service's numerical failure triggers.

## Frontend tests

Vitest runs colocated tests in jsdom, with Testing Library, user-event, and jest-dom assertions. `src/test/setup.ts` cleans up rendered components; Vite's test configuration restores mocks and unstubs globals between tests.

- `loanValidation.test.ts` verifies request construction, required fields, real calendar dates, ordering, positive principal, fractional precision, and nonnegative interest.
- `format.test.ts` checks BRL and percentage formatting, date display without time-zone shifts, leap-date validity, Brazilian number parsing, dot interpretation, and malformed inputs.
- `loanApi.test.ts` stubs `fetch` to check endpoint/payload serialization, HTTP and network errors, malformed top-level response shapes, status-specific messaging, and suppression of server internals.
- `LoanCalculator.test.tsx` exercises the composed form and result UI: validation feedback, loading state, request payload, input formatting, table order and cells, backend-supplied totals, empty schedules, errors, retry, stale-result hiding, and clearing.

Component response fixtures contain arbitrary presentation values; they are not financial references. Frontend tests do not calculate amortization, contact a running backend, or test Nginx/Vite forwarding. The code implements abort handling, but the suite has no dedicated in-flight cancellation/race regression test.

## Execution checks and limitations

The repository has no committed browser end-to-end or Docker test harness. For a manual integration check, use the execution commands in the [README](../README.md), submit its example loan, and verify rendered results plus an HTTP 200 API call through the same public origin. In Docker, confirm that frontend port 3000 and backend port 8081 are published; open `http://localhost:8081/swagger-ui.html`, try the calculation endpoint, and stop with `docker compose down`. Locally, verify requests through Vite on port 5173 with Spring Boot and Swagger UI on port 8080.

The backend Docker build runs Maven `verify`; the frontend Docker build runs `npm ci` and the production build, but not frontend tests or lint. Neither build alone validates browser behavior. Current automated scope does not establish comprehensive holiday handling, broad extreme-value behavior, performance/load limits, real-browser compatibility, complete accessibility conformance, or a production security baseline.
