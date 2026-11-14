import { describe, expect, it } from 'vitest'
import {
  formatCurrency,
  formatIsoDate,
  formatPercent,
  isValidIsoDate,
  parseBrazilianDecimal,
} from './format'

describe('formatCurrency', () => {
  it('formats values as BRL', () => {
    expect(formatCurrency(140000)).toBe('R$ 140.000,00')
    expect(formatCurrency(0)).toBe('R$ 0,00')
    expect(formatCurrency(1234.5)).toBe('R$ 1.234,50')
  })
})

describe('formatPercent', () => {
  it('formats percentage points with at least two decimals', () => {
    expect(formatPercent(7)).toBe('7,00%')
    expect(formatPercent(7.125)).toBe('7,125%')
  })
})

describe('formatIsoDate', () => {
  it('converts ISO dates to DD/MM/AAAA without time zone shifts', () => {
    expect(formatIsoDate('2024-01-01')).toBe('01/01/2024')
    expect(formatIsoDate('2034-12-31')).toBe('31/12/2034')
  })
})

describe('isValidIsoDate', () => {
  it('accepts real calendar dates only', () => {
    expect(isValidIsoDate('2024-02-29')).toBe(true)
    expect(isValidIsoDate('2023-02-29')).toBe(false)
    expect(isValidIsoDate('2024-13-01')).toBe(false)
    expect(isValidIsoDate('01/01/2024')).toBe(false)
    expect(isValidIsoDate('')).toBe(false)
  })
})

describe('parseBrazilianDecimal', () => {
  it.each([
    ['140.000,00', 140000, 2],
    ['140000,5', 140000.5, 1],
    ['140000.50', 140000.5, 2],
    ['140.000', 140000, 0],
    ['R$ 1.234,56', 1234.56, 2],
    ['7%', 7, 0],
    ['-3,5', -3.5, 1],
  ])('parses %s', (input, value, fractionDigits) => {
    expect(parseBrazilianDecimal(input)).toEqual({ status: 'ok', value, fractionDigits })
  })

  it('treats dots as decimals when grouping is disabled', () => {
    expect(parseBrazilianDecimal('7.125', { dotAsGroupSeparator: false })).toEqual({
      status: 'ok',
      value: 7.125,
      fractionDigits: 3,
    })
  })

  it('reports empty and invalid input', () => {
    expect(parseBrazilianDecimal('  ')).toEqual({ status: 'empty' })
    expect(parseBrazilianDecimal('abc')).toEqual({ status: 'invalid' })
    expect(parseBrazilianDecimal('1,2,3')).toEqual({ status: 'invalid' })
    expect(parseBrazilianDecimal('14.00,00')).toEqual({ status: 'invalid' })
  })
})
