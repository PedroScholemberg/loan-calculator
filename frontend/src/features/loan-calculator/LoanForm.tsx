import { useRef, useState, type ChangeEvent, type FormEvent } from 'react'
import { Button } from '../../components/Button'
import { FormField } from '../../components/FormField'
import { formatDecimal } from '../../utils/format'
import type { AmortizationScheduleRequest } from './loan.types'
import {
  EMPTY_LOAN_FORM,
  LOAN_FORM_FIELDS,
  parseInterestRate,
  parsePrincipal,
  validateLoanForm,
  type LoanFormErrors,
  type LoanFormField,
  type LoanFormValues,
} from './loanValidation'

interface LoanFormProps {
  isSubmitting: boolean
  /** Called with a validated request when the user asks for a calculation. */
  onSubmit: (request: AmortizationScheduleRequest) => void
  /** Called whenever a parameter value changes. */
  onChange: () => void
  /** Called after the form has been cleared. */
  onClear: () => void
}

// Keeps browsers from accepting years with more than four digits.
const DATE_LIMITS = { min: '1900-01-01', max: '9999-12-31' }

export function LoanForm({ isSubmitting, onSubmit, onChange, onClear }: LoanFormProps) {
  const [values, setValues] = useState<LoanFormValues>(EMPTY_LOAN_FORM)
  const [errors, setErrors] = useState<LoanFormErrors>({})
  // After the first submit attempt, errors are re-evaluated on every change.
  const [hasSubmitted, setHasSubmitted] = useState(false)
  const inputRefs = useRef<Partial<Record<LoanFormField, HTMLInputElement | null>>>({})

  function updateValue(field: LoanFormField, value: string) {
    const next = { ...values, [field]: value }
    setValues(next)
    if (hasSubmitted) {
      const result = validateLoanForm(next)
      setErrors(result.valid ? {} : result.errors)
    }
  }

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    updateValue(event.target.name as LoanFormField, event.target.value)
    onChange()
  }

  /** Rewrites numeric inputs in pt-BR notation (e.g. `140000` → `140.000,00`) without changing their value. */
  function normalizePrincipal() {
    const parsed = parsePrincipal(values.principal)
    if (parsed.status === 'ok' && parsed.fractionDigits <= 2) {
      updateValue('principal', formatDecimal(parsed.value))
    }
  }

  function normalizeInterestRate() {
    const parsed = parseInterestRate(values.annualInterestRate)
    if (parsed.status === 'ok') {
      updateValue('annualInterestRate', formatDecimal(parsed.value, 2, 10))
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSubmitting) return

    setHasSubmitted(true)
    const result = validateLoanForm(values)
    if (!result.valid) {
      setErrors(result.errors)
      const firstInvalid = LOAN_FORM_FIELDS.find((field) => result.errors[field])
      if (firstInvalid) inputRefs.current[firstInvalid]?.focus()
      return
    }

    setErrors({})
    onSubmit(result.request)
  }

  function handleClear() {
    setValues(EMPTY_LOAN_FORM)
    setErrors({})
    setHasSubmitted(false)
    onClear()
    inputRefs.current.startDate?.focus()
  }

  const fieldProps = (field: LoanFormField) => ({
    id: field,
    value: values[field],
    error: errors[field],
    onChange: handleChange,
    required: true,
    ref: (element: HTMLInputElement | null) => {
      inputRefs.current[field] = element
    },
  })

  return (
    <section className="panel" aria-labelledby="loan-form-title">
      <h2 id="loan-form-title" className="panel__title">
        Parâmetros do empréstimo
      </h2>
      <p className="panel__description">Todos os campos são obrigatórios.</p>

      <form className="loan-form" onSubmit={handleSubmit} noValidate>
        <div className="loan-form__row loan-form__row--dates">
          <FormField {...fieldProps('startDate')} label="Data inicial" type="date" {...DATE_LIMITS} />
          <FormField {...fieldProps('endDate')} label="Data final" type="date" {...DATE_LIMITS} />
          <FormField
            {...fieldProps('firstPaymentDate')}
            label="Primeiro pagamento"
            type="date"
            {...DATE_LIMITS}
          />
        </div>

        <div className="loan-form__row loan-form__row--values">
          <FormField
            {...fieldProps('principal')}
            label="Valor do empréstimo"
            type="text"
            inputMode="decimal"
            autoComplete="off"
            placeholder="140.000,00"
            prefix="R$"
            hint="Valor em reais. Exemplo: 140.000,00"
            onBlur={normalizePrincipal}
          />
          <FormField
            {...fieldProps('annualInterestRate')}
            label="Taxa de juros anual (%)"
            type="text"
            inputMode="decimal"
            autoComplete="off"
            placeholder="7,00"
            suffix="%"
            hint="Percentual ao ano. Exemplo: 7,00"
            onBlur={normalizeInterestRate}
          />
        </div>

        <div className="loan-form__actions">
          <Button variant="secondary" onClick={handleClear}>
            Limpar
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? 'Calculando…' : 'Calcular amortização'}
          </Button>
        </div>
      </form>
    </section>
  )
}
