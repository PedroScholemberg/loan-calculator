import { formatCurrency, formatIsoDate } from '../../utils/format'
import type { ScheduleEntry } from './loan.types'

const PAYMENT_COLUMNS = ['totalPayment', 'amortization', 'remainingPrincipal',
  'interestProvision', 'accumulatedInterest', 'paidInterest'] as const

interface AmortizationTableProps {
  schedule: ScheduleEntry[]
  installmentCount: number
}

export function AmortizationTable({ schedule, installmentCount }: AmortizationTableProps) {
  if (schedule.length === 0) {
    return <p className="message">Nenhum evento foi retornado para os parâmetros informados.</p>
  }
  return (
    <div className="table-scroll" role="region" aria-labelledby="schedule-caption" tabIndex={0}>
      <table className="schedule">
        <caption id="schedule-caption" className="schedule__caption">
          Eventos financeiros do empréstimo ({schedule.length}{' '}
          {schedule.length === 1 ? 'evento' : 'eventos'}), valores em reais.
        </caption>
        <thead>
          <tr>
            <th scope="col" rowSpan={2}>Data Competência</th>
            <th scope="colgroup" colSpan={2}>Empréstimo</th>
            <th scope="colgroup" colSpan={2}>Parcela</th>
            <th scope="colgroup" colSpan={2}>Principal</th>
            <th scope="colgroup" colSpan={3}>Juros</th>
          </tr>
          <tr>
            {['Valor de Empréstimo', 'Saldo Devedor', 'Consolidada', 'Total',
              'Amortização', 'Saldo', 'Provisão', 'Acumulado', 'Pago'].map((label) => (
              <th key={label} scope="col" className="schedule__number">{label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {schedule.map((entry, index) => (
            <tr key={index} className={`schedule__row schedule__row--${entry.eventType}`}>
              <th scope="row" className="schedule__date">{formatIsoDate(entry.date)}</th>
              <td className="schedule__number">{index === 0 ? formatCurrency(entry.loanAmount) : ''}</td>
              <td className="schedule__number">{formatCurrency(entry.outstandingBalance)}</td>
              <td className="schedule__number">
                {entry.installmentNumber === null ? '' : `${entry.installmentNumber}/${installmentCount}`}
              </td>
              {PAYMENT_COLUMNS.map((key) => (
                <td key={key} className="schedule__number">{formatCurrency(entry[key])}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
