/**
 * Types for the backend contract `POST /api/v1/loans/amortization-schedule`.
 * See docs/business-rules.md for API behavior and field meanings.
 */

/** ISO-8601 calendar date (`YYYY-MM-DD`), with no time or time zone. */
export type IsoDate = string

export interface AmortizationScheduleRequest {
  startDate: IsoDate
  endDate: IsoDate
  firstPaymentDate: IsoDate
  /** Loan principal in BRL, for example `140000.00`. */
  principal: number
  /** Annual interest rate in percentage points: `7.0` means 7% per year. */
  annualInterestRate: number
}

export const EVENT_TYPES = [
  'LOAN_START',
  'MONTH_END',
  'PAYMENT',
  'LOAN_END',
] as const

export type ScheduleEventType = (typeof EVENT_TYPES)[number]

export interface ScheduleEntry {
  date: IsoDate
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

export interface ScheduleSummary {
  installmentCount: number
  totalInterest: number
  totalAmortization: number
  totalPayment: number
}

export interface AmortizationScheduleResponse {
  schedule: ScheduleEntry[]
  summary: ScheduleSummary
}

/** Error body (RFC 9457 Problem Details, as produced by Spring's `ProblemDetail`). */
export interface ApiProblem {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  errors?: { field: string; message: string }[]
}
