import { formatCurrency, formatIsoDate } from '../../utils/format'
import type { ScheduleEntry, ScheduleEventType } from './loan.types'

const EVENT_LABELS: Record<ScheduleEventType, string> = {
  LOAN_START: 'Início do empréstimo',
  MONTH_END: 'Fechamento mensal',
  PAYMENT: 'Pagamento',
  LOAN_END: 'Encerramento',
}

const MONEY_COLUMNS = [
  { key: 'openingBalance', label: 'Saldo inicial' },
  { key: 'interest', label: 'Juros' },
  { key: 'amortization', label: 'Amortização' },
  { key: 'payment', label: 'Pagamento' },
  { key: 'closingBalance', label: 'Saldo final' },
] as const satisfies readonly { key: keyof ScheduleEntry; label: string }[]

interface AmortizationTableProps {
  /** Rows in the order returned by the backend. */
  schedule: ScheduleEntry[]
}

export function AmortizationTable({ schedule }: AmortizationTableProps) {
  if (schedule.length === 0) {
    return (
      <p className="message">
        Nenhum evento foi retornado para os parâmetros informados.
      </p>
    )
  }

  return (
    // Focusable so keyboard users can scroll the table horizontally.
    <div
      className="table-scroll"
      role="region"
      aria-labelledby="schedule-caption"
      tabIndex={0}
    >
      <table className="schedule">
        <caption id="schedule-caption" className="schedule__caption">
          Eventos financeiros do empréstimo ({schedule.length}{' '}
          {schedule.length === 1 ? 'evento' : 'eventos'}), valores em reais.
        </caption>
        <thead>
          <tr>
            <th scope="col">Data</th>
            <th scope="col">Evento</th>
            {MONEY_COLUMNS.map(({ key, label }) => (
              <th key={key} scope="col" className="schedule__number">
                {label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {schedule.map((entry, index) => (
            // The list is replaced as a whole on each response, so the index is a stable key.
            <tr key={index} className={`schedule__row schedule__row--${entry.eventType}`}>
              <th scope="row" className="schedule__date">
                {formatIsoDate(entry.date)}
              </th>
              <td>
                <span className={`event-badge event-badge--${entry.eventType}`}>
                  {EVENT_LABELS[entry.eventType] ?? entry.eventType}
                </span>
              </td>
              {MONEY_COLUMNS.map(({ key }) => (
                <td key={key} className="schedule__number">
                  {formatCurrency(entry[key])}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
