import { describe, expect, it } from 'vitest'
import { EMPTY_LOAN_FORM, validateLoanForm, type LoanFormValues } from './loanValidation'

const VALID_VALUES: LoanFormValues = {
  startDate: '2024-01-01',
  endDate: '2034-01-01',
  firstPaymentDate: '2024-02-15',
  principal: '140.000,00',
  annualInterestRate: '7,00',
}

function errorsFor(overrides: Partial<LoanFormValues>) {
  const result = validateLoanForm({ ...VALID_VALUES, ...overrides })
  return result.valid ? {} : result.errors
}

describe('validateLoanForm', () => {
  it('builds the API request from valid values', () => {
    expect(validateLoanForm(VALID_VALUES)).toEqual({
      valid: true,
      request: {
        startDate: '2024-01-01',
        endDate: '2034-01-01',
        firstPaymentDate: '2024-02-15',
        principal: 140000,
        annualInterestRate: 7,
      },
    })
  })

  it('requires every field', () => {
    const result = validateLoanForm(EMPTY_LOAN_FORM)
    expect(result.valid).toBe(false)
    if (result.valid) return
    expect(Object.keys(result.errors).sort()).toEqual(
      ['annualInterestRate', 'endDate', 'firstPaymentDate', 'principal', 'startDate'].sort(),
    )
  })

  it('rejects impossible dates', () => {
    expect(errorsFor({ startDate: '2023-02-30' }).startDate).toBe(
      'Informe uma data inicial válida.',
    )
  })

  it('requires the end date to be after the start date', () => {
    expect(errorsFor({ endDate: '2024-01-01' }).endDate).toBe(
      'A data final deve ser posterior à data inicial.',
    )
    expect(errorsFor({ endDate: '2023-12-31' }).endDate).toBeDefined()
  })

  it('requires the first payment to be after the start date and before the end date', () => {
    expect(errorsFor({ firstPaymentDate: '2024-01-01' }).firstPaymentDate).toBe(
      'O primeiro pagamento deve ser posterior à data inicial.',
    )
    expect(errorsFor({ firstPaymentDate: '2034-01-02' }).firstPaymentDate).toBe(
      'O primeiro pagamento deve ser anterior à data final.',
    )
    expect(errorsFor({ firstPaymentDate: '2034-01-01' }).firstPaymentDate).toBe('O primeiro pagamento deve ser anterior à data final.')
  })

  it('requires a positive principal with at most two decimals', () => {
    expect(errorsFor({ principal: '0' }).principal).toBe(
      'O valor do empréstimo deve ser maior que zero.',
    )
    expect(errorsFor({ principal: '-10' }).principal).toBeDefined()
    expect(errorsFor({ principal: 'dez mil' }).principal).toBe(
      'Informe um valor numérico válido, por exemplo 140.000,00.',
    )
    expect(errorsFor({ principal: '100,123' }).principal).toBe(
      'Informe o valor com no máximo 2 casas decimais.',
    )
  })

  it('rejects negative interest rates but accepts zero', () => {
    expect(errorsFor({ annualInterestRate: '-1' }).annualInterestRate).toBe(
      'A taxa de juros não pode ser negativa.',
    )
    expect(errorsFor({ annualInterestRate: '0' })).toEqual({})
  })
})
