import { formatCurrency } from '../../utils/format'
import type { ScheduleSummary } from './loan.types'

interface LoanSummaryProps {
  /** Totals exactly as returned by the backend; never computed here. */
  summary: ScheduleSummary
}

export function LoanSummary({ summary }: LoanSummaryProps) {
  const items = [
    { label: 'Total de juros', value: summary.totalInterest },
    { label: 'Total amortizado', value: summary.totalAmortization },
    { label: 'Total pago', value: summary.totalPayment },
  ]

  return (
    <section aria-label="Resumo financeiro">
      <dl className="summary">
        {items.map(({ label, value }) => (
          <div key={label} className="summary__item">
            <dt className="summary__label">{label}</dt>
            <dd className="summary__value">{formatCurrency(value)}</dd>
          </div>
        ))}
      </dl>
    </section>
  )
}
