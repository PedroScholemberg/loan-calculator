# Frontend — Calculadora de Amortização

React 19 + TypeScript + Vite single-page app. Users enter loan parameters and
get the amortization schedule calculated by the Spring Boot backend. The
frontend performs **no financial calculations**; it validates input, calls
the API, and formats the response for Brazilian users.

The API contract the backend must implement is documented in
[`BACKEND_CONTRACT.md`](./BACKEND_CONTRACT.md).

## Scripts

```bash
npm ci               # install dependencies
npm run dev          # dev server on http://localhost:5173
npm test             # run the tests once (Vitest + Testing Library)
npm run test:watch   # run the tests in watch mode
npm run typecheck    # TypeScript project check
npm run lint         # Oxlint
npm run build        # typecheck and production build into dist/
npm run preview      # serve the production build on http://localhost:4173
```

## Configuration

| Variable | Default | Description |
| --- | --- | --- |
| `VITE_API_BASE_URL` | Same origin | Optional base URL of the Spring Boot API |

Vite proxies `/api` to `http://localhost:8080` during development. No environment file is required. Copy `.env.example` to `.env.local` only to override the API origin. The value is
read at build time, so set it before `npm run build` for other environments.

## Structure

```text
src/
├── api/httpClient.ts              # fetch wrapper: JSON, ApiError, network errors
├── config/env.ts                  # API base URL from VITE_API_BASE_URL
├── components/                    # Presentational, feature-agnostic
│   ├── Button.tsx
│   └── FormField.tsx              # label + input + hint + accessible error
├── features/loan-calculator/
│   ├── LoanCalculator.tsx         # Composition and request state (idle/loading/success/error/stale)
│   ├── LoanForm.tsx               # Form state, validation display, submit/clear
│   ├── LoanSummary.tsx            # Totals returned by the API
│   ├── AmortizationTable.tsx      # Schedule table
│   ├── loanApi.ts                 # POST /api/v1/loans/amortization-schedule + error messages
│   ├── loanValidation.ts          # Pure validation, builds the request payload
│   └── loan.types.ts              # Request/response types (API contract)
├── utils/format.ts                # pt-BR currency/date/percent formatting and number parsing
└── test/setup.ts                  # Testing Library / jest-dom setup
```

Tests sit next to the code they cover (`*.test.ts(x)`). The API is mocked
only in tests, by stubbing `fetch`.

## Behaviour notes

- Dates use native `<input type="date">`, which shows `DD/MM/AAAA` in a pt-BR
  browser and always yields `YYYY-MM-DD`, the format sent to the API.
- Amounts accept `140.000,00`, `140000,00` or `140000` and are normalized to
  `140.000,00` on blur. The rate is normalized to `7,00`. Both are sent as
  plain JSON numbers.
- Editing any parameter after a calculation hides the previous result and
  cancels any request in flight, so a stale schedule is never shown next to
  new inputs.
- Errors from the API are mapped to user-friendly messages. Details from
  `500` responses are never shown.
