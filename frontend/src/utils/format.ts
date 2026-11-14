/**
 * Brazilian (pt-BR) display formatting and input parsing.
 * These helpers only convert representations; they never compute financial values.
 */

const LOCALE = 'pt-BR'

const currencyFormatter = new Intl.NumberFormat(LOCALE, {
  style: 'currency',
  currency: 'BRL',
})

/** `140000` → `R$ 140.000,00` */
export function formatCurrency(value: number): string {
  return currencyFormatter.format(value)
}

/** `140000` → `140.000,00` */
export function formatDecimal(
  value: number,
  minimumFractionDigits = 2,
  maximumFractionDigits = 2,
): string {
  return new Intl.NumberFormat(LOCALE, {
    minimumFractionDigits,
    maximumFractionDigits,
  }).format(value)
}

/** Percentage points to display text: `7` → `7,00%`, `7.125` → `7,125%`. */
export function formatPercent(value: number): string {
  return `${formatDecimal(value, 2, 10)}%`
}

const ISO_DATE = /^(\d{4})-(\d{2})-(\d{2})$/

/** `2024-01-31` → `31/01/2024`. Parsed as text to avoid time zone shifts. */
export function formatIsoDate(isoDate: string): string {
  const match = ISO_DATE.exec(isoDate)
  if (!match) return isoDate
  const [, year, month, day] = match
  return `${day}/${month}/${year}`
}

/** True when `value` is a real calendar date in `YYYY-MM-DD` format. */
export function isValidIsoDate(value: string): boolean {
  const match = ISO_DATE.exec(value)
  if (!match) return false
  const [year, month, day] = match.slice(1).map(Number)
  const date = new Date(Date.UTC(year, month - 1, day))
  return (
    date.getUTCFullYear() === year &&
    date.getUTCMonth() === month - 1 &&
    date.getUTCDate() === day
  )
}

export type ParsedDecimal =
  | { status: 'empty' }
  | { status: 'invalid' }
  | { status: 'ok'; value: number; fractionDigits: number }

interface ParseOptions {
  /**
   * Treat dots as thousands separators even without a decimal comma
   * (`140.000` → 140000). Disable for fields where `7.5` means 7.5.
   */
  dotAsGroupSeparator?: boolean
}

const GROUPED_WITH_COMMA = /^\d{1,3}(\.\d{3})+,\d+$/
const PLAIN_WITH_COMMA = /^\d+,\d+$/
const GROUPED_INTEGER = /^\d{1,3}(\.\d{3})+$/
const PLAIN_NUMBER = /^\d+(\.\d+)?$/

/**
 * Parses numbers typed the Brazilian way: `140.000,50`, `140000,50`,
 * `140000.50` or `140000`. A leading `R$` or a trailing `%` is ignored.
 */
export function parseBrazilianDecimal(
  input: string,
  { dotAsGroupSeparator = true }: ParseOptions = {},
): ParsedDecimal {
  const text = input.replace(/\s/g, '').replace(/^R\$/i, '').replace(/%$/, '')
  if (!text) return { status: 'empty' }

  const negative = text.startsWith('-')
  const body = negative ? text.slice(1) : text

  let normalized: string
  if (GROUPED_WITH_COMMA.test(body) || PLAIN_WITH_COMMA.test(body)) {
    normalized = body.replace(/\./g, '').replace(',', '.')
  } else if (dotAsGroupSeparator && GROUPED_INTEGER.test(body)) {
    normalized = body.replace(/\./g, '')
  } else if (PLAIN_NUMBER.test(body)) {
    normalized = body
  } else {
    return { status: 'invalid' }
  }

  const fractionDigits = normalized.split('.')[1]?.length ?? 0
  const value = Number(normalized)
  return { status: 'ok', value: negative ? -value : value, fractionDigits }
}
