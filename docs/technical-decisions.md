# Technical Decisions and Trade-offs

This document describes characteristics and consequences visible in the implementation. It does not infer historical motivations. Exact financial behavior is specified in [business rules](business-rules.md).

## Java, Spring Boot, and backend boundaries

[pom.xml](../backend/pom.xml) targets Java 17 and uses Spring Boot 4.0.9 with MVC, Bean Validation, and their test starters. Maven Wrapper supplies the build tool; the executable JAR runs an embedded HTTP server. Framework-managed JSON binding, validation, and exception advice keep transport handling outside the calculation domain, at the cost of a framework-dependent API layer and JVM runtime.

The controller delegates to one application service. Scheduling and calculation are separate plain-Java domain operations, allowing arithmetic to be tested on a supplied schedule independently of calendar generation. This is a package-based separation within one application, not independently deployed backend modules. Mutable domain holders are less restrictive than immutable models, and callers invoking them directly bypass HTTP validation.

## Decimal arithmetic and fractional growth

`BigDecimal` stores principal, interest, and balances. Decimal input and finite-precision division avoid using binary floating point for all monetary operations. A 34-digit `HALF_EVEN` context is applied to division and provision multiplication; intermediate amounts are not rounded to cents.

Fractional exponentiation is a deliberate precision boundary visible in the code: it uses `double` with `StrictMath.log1p` and `expm1`, then converts the result back to `BigDecimal`. This provides numerically stable small-growth evaluation but does not make the calculation wholly decimal or arbitrarily precise. Last digits can differ from the spreadsheet reference. Very large rates or long periods can exceed numerical capacity; calculation failures map to HTTP 422. No explicit request-size policy caps the financial term or input magnitude.

The final payment amortizes all remaining principal, which removes finite-division residue. Two-decimal `HALF_EVEN` rounding belongs to response mapping rather than domain evolution. Summing unrounded values preserves the calculation's totals but means displayed row sums may differ from summary totals. The Excel comparison uses `HALF_UP` at display precision; that test convention is distinct from API rounding and does not verify half-even ties at the service boundary.

The browser uses JavaScript numbers for input and JSON output values. Formatting is presentation only; the backend remains authoritative. Large values can lose precision in the browser even though backend monetary fields use `BigDecimal`.

## Dates and calendars

`LocalDate` represents calendar dates without time-zone conversion. `YearMonth` handles month length, leap years, and month-end anchors. A sorted date map merges coincident events and gives the calculator chronological unique dates. Monthly recurrence is anchored before weekend adjustment, so shifts do not accumulate across months.

Only monthly candidates receive weekend adjustment. The mandatory final payment remains on the exact end date. This distinction is explicit in the code and scheduler tests; treating every payment as a business-day-adjusted date would misdescribe the current system.

### Holiday options

Only the last option below is implemented. These alternatives describe possible approaches, not available configuration or planned features.

| Approach | Maintenance | Reliability and complexity | Limitation |
| --- | --- | --- | --- |
| Internal holiday dataset | Maintain dates, jurisdictions, exceptional holidays, and future years. | Works offline with versioned inputs; requires update and correctness checks. | An outdated or incomplete dataset silently produces incorrect dates. |
| External holiday API | Maintain provider integration, error handling, and contract compatibility. | Introduces network availability, latency, caching, and reproducibility concerns. | Coverage and correctness depend on the provider; outages need an explicit policy. |
| Configurable calendars | Define a configuration format, ownership, validation, and versioning. | Can support different jurisdictions without code changes; adds selection and audit complexity. | Users/operators must supply complete, consistent dates. |
| Current weekend-only calendar | No holiday data or provider upkeep. | Deterministic, offline, and small implementation. | Weekday holidays and holiday Mondays are treated as ordinary dates. |

The repository contains no calendar configuration or holiday dependency. Calendar differences from the Excel fixture are isolated from formula comparisons. Selecting a broader calendar would require specifying jurisdiction, supported years, final-date behavior, and data ownership; those requirements cannot be inferred from the current implementation.

## Validation and errors

Bean Validation on the request record is the server boundary; frontend validation provides earlier field feedback. Duplicating checks improves usability but permits differences such as typed trailing-zero precision and UI date bounds. The backend does not rely on browser validation.

Spring `ProblemDetail` centralizes error responses. Invalid input is distinguished from uncalculable inputs and unexpected failures using 400, 422, and 500. Expected calculation causes remain internal; unexpected exceptions are logged and produce a generic message. The frontend uses server detail only for 400/422 and hides 500 internals. These messages are Portuguese; no localization mechanism is implemented. There is no structured server field-error mapping into the form.

## Frontend organization

React hooks manage feature-local request state without a router or state-management dependency. TypeScript types, feature-specific API code, shared HTTP handling, and formatting helpers separate concerns without a monorepo orchestration framework. The trade-off is that request coordination and validation are implemented directly in the feature rather than provided by a query/form library.

Requests use `AbortController`, and input changes invalidate the displayed result. This avoids showing a schedule against edited parameters. Cancellation does not guarantee that the backend stops processing an already received request. Runtime response checks are shallow; TypeScript declarations do not validate all received fields.

Vite builds static assets, and Nginx serves them without Node.js in the runtime container. Plain CSS and semantic controls/table markup support responsive presentation and keyboard interaction; jsdom tests do not establish complete browser or accessibility conformance.

## Deployment and stateless operation

Compose supplies a two-container setup. Nginx on host port 3000 handles static files, SPA fallback, and API forwarding. Backend container port 8080 is also published on host port 8081 for direct API access and Swagger UI. The frontend continues to use its same-origin API proxy without requiring browser-visible Docker names or CORS changes. Multi-stage builds separate compilation tools from runtime images. First builds still require dependency and base-image access, and moving base-image tags can change future builds.

springdoc-openapi generates the OpenAPI definition at `/v3/api-docs` and serves Swagger UI at `/swagger-ui.html`. Controller annotations describe success and Problem Details responses; request annotations supply field descriptions and usable examples while hiding validation helper properties from the schema.

The backend readiness probe reuses `OPTIONS` on the existing endpoint instead of introducing a health endpoint. It gates frontend startup but does not test financial correctness or provide automatic recovery from later failures. Nginx's Docker DNS resolver accommodates backend address replacement.

For local development, Vite replaces Nginx's API-proxy role, with no source changes or required environment files. Preview has no API proxy configured; an explicit API origin or Nginx is needed for a connected production preview.

All simulation state lives in the current request and browser component memory. No database or external service is necessary to calculate a schedule. Consequently there is no persistence, audit trail, account management, or saved-result retrieval, and large schedules are materialized fully in memory. The implementation includes no documented performance envelope or production-readiness evidence.
