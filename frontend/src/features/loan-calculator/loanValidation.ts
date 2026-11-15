import { isValidIsoDate, parseBrazilianDecimal } from '../../utils/format'
import type { AmortizationScheduleRequest } from './loan.types'

/** Raw form values, exactly as typed by the user. */
export interface LoanFormValues {
  startDate: string
  endDate: string
  firstPaymentDate: string
  principal: string
  annualInterestRate: string
}

export type LoanFormField = keyof LoanFormValues

export type LoanFormErrors = Partial<Record<LoanFormField, string>>

export const EMPTY_LOAN_FORM: LoanFormValues = {
  startDate: '',
  endDate: '',
  firstPaymentDate: '',
  principal: '',
  annualInterestRate: '',
}

/** Field order, used to focus the first invalid field. */
export const LOAN_FORM_FIELDS: LoanFormField[] = [
  'startDate',
  'endDate',
  'firstPaymentDate',
  'principal',
  'annualInterestRate',
]

const MAX_PRINCIPAL_FRACTION_DIGITS = 2

export const parsePrincipal = (input: string) => parseBrazilianDecimal(input)

export const parseInterestRate = (input: string) =>
  parseBrazilianDecimal(input, { dotAsGroupSeparator: false })

function validateDates(values: LoanFormValues, errors: LoanFormErrors) {
  const { startDate, endDate, firstPaymentDate } = values
  const startValid = isValidIsoDate(startDate)
  const endValid = isValidIsoDate(endDate)
  const firstPaymentValid = isValidIsoDate(firstPaymentDate)

  if (!startValid) errors.startDate = 'Informe uma data inicial válida.'
  if (!endValid) errors.endDate = 'Informe uma data final válida.'
  if (!firstPaymentValid) {
    errors.firstPaymentDate = 'Informe uma data de primeiro pagamento válida.'
  }

  // ISO dates (YYYY-MM-DD) compare correctly as strings.
  if (startValid && endValid && endDate <= startDate) {
    errors.endDate = 'A data final deve ser posterior à data inicial.'
  }
  if (startValid && firstPaymentValid && firstPaymentDate <= startDate) {
    errors.firstPaymentDate = 'O primeiro pagamento deve ser posterior à data inicial.'
  } else if (endValid && firstPaymentValid && firstPaymentDate >= endDate) {
    errors.firstPaymentDate = 'O primeiro pagamento deve ser anterior à data final.'
  }
}

function validatePrincipal(input: string): string | undefined {
  const parsed = parsePrincipal(input)
  if (parsed.status === 'empty') return 'Informe o valor do empréstimo.'
  if (parsed.status === 'invalid') {
    return 'Informe um valor numérico válido, por exemplo 140.000,00.'
  }
  if (parsed.value <= 0) return 'O valor do empréstimo deve ser maior que zero.'
  if (parsed.fractionDigits > MAX_PRINCIPAL_FRACTION_DIGITS) {
    return 'Informe o valor com no máximo 2 casas decimais.'
  }
  return undefined
}

function validateInterestRate(input: string): string | undefined {
  const parsed = parseInterestRate(input)
  if (parsed.status === 'empty') return 'Informe a taxa de juros anual.'
  if (parsed.status === 'invalid') {
    return 'Informe uma taxa numérica válida, por exemplo 7,00.'
  }
  if (parsed.value < 0) return 'A taxa de juros não pode ser negativa.'
  return undefined
}

export type LoanFormValidation =
  | { valid: true; request: AmortizationScheduleRequest }
  | { valid: false; errors: LoanFormErrors }

/** Validates the raw form values and, when valid, builds the API request. */
export function validateLoanForm(values: LoanFormValues): LoanFormValidation {
  const errors: LoanFormErrors = {}
  validateDates(values, errors)

  const principalError = validatePrincipal(values.principal)
  if (principalError) errors.principal = principalError

  const rateError = validateInterestRate(values.annualInterestRate)
  if (rateError) errors.annualInterestRate = rateError

  const principal = parsePrincipal(values.principal)
  const rate = parseInterestRate(values.annualInterestRate)
  if (Object.keys(errors).length > 0 || principal.status !== 'ok' || rate.status !== 'ok') {
    return { valid: false, errors }
  }

  return {
    valid: true,
    request: {
      startDate: values.startDate,
      endDate: values.endDate,
      firstPaymentDate: values.firstPaymentDate,
      principal: principal.value,
      annualInterestRate: rate.value,
    },
  }
}
