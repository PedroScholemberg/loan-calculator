import { useEffect, useRef, useState } from 'react'
import { Button } from '../../components/Button'
import { AmortizationTable } from './AmortizationTable'
import type { AmortizationScheduleRequest, AmortizationScheduleResponse } from './loan.types'
import { fetchAmortizationSchedule, getErrorMessage } from './loanApi'
import { LoanForm } from './LoanForm'
import { LoanSummary } from './LoanSummary'

type CalculationState =
  | { status: 'idle' }
  /** Parameters changed after a calculation; the previous result is no longer valid. */
  | { status: 'stale' }
  | { status: 'loading' }
  | { status: 'success'; result: AmortizationScheduleResponse }
  | { status: 'error'; message: string; request: AmortizationScheduleRequest }

const STATUS_MESSAGES: Partial<Record<CalculationState['status'], string>> = {
  idle: 'Preencha os parâmetros do empréstimo e clique em “Calcular amortização” para visualizar o cronograma.',
  stale: 'Os parâmetros foram alterados. Clique em “Calcular amortização” para gerar um novo cronograma.',
  loading: 'Calculando o cronograma de amortização…',
}

export function LoanCalculator() {
  const [state, setState] = useState<CalculationState>({ status: 'idle' })
  const pendingRequest = useRef<AbortController | null>(null)

  useEffect(() => () => pendingRequest.current?.abort(), [])

  function cancelPendingRequest() {
    pendingRequest.current?.abort()
    pendingRequest.current = null
  }

  async function calculate(request: AmortizationScheduleRequest) {
    cancelPendingRequest()
    const controller = new AbortController()
    pendingRequest.current = controller
    setState({ status: 'loading' })

    try {
      const result = await fetchAmortizationSchedule(request, controller.signal)
      if (!controller.signal.aborted) setState({ status: 'success', result })
    } catch (error) {
      if (!controller.signal.aborted) {
        setState({ status: 'error', message: getErrorMessage(error), request })
      }
    } finally {
      if (pendingRequest.current === controller) pendingRequest.current = null
    }
  }

  function handleParametersChange() {
    cancelPendingRequest()
    setState((current) => (current.status === 'idle' ? current : { status: 'stale' }))
  }

  function handleClear() {
    cancelPendingRequest()
    setState({ status: 'idle' })
  }

  const isLoading = state.status === 'loading'
  const statusMessage =
    state.status === 'success'
      ? `Cronograma calculado com ${state.result.schedule.length} ${
          state.result.schedule.length === 1 ? 'evento' : 'eventos'
        }.`
      : STATUS_MESSAGES[state.status]

  return (
    <div className="loan-calculator">
      <LoanForm
        isSubmitting={isLoading}
        onSubmit={(request) => void calculate(request)}
        onChange={handleParametersChange}
        onClear={handleClear}
      />

      <section className="panel" aria-labelledby="results-title" aria-busy={isLoading}>
        <h2 id="results-title" className="panel__title">
          Resultado da simulação
        </h2>

        {/* Live regions stay mounted so screen readers announce changes. */}
        <p
          role="status"
          aria-live="polite"
          className={state.status === 'success' ? 'visually-hidden' : 'message'}
        >
          {statusMessage}
        </p>

        <div role="alert">
          {state.status === 'error' && (
            <div className="message message--error">
              <p>{state.message}</p>
              <Button variant="secondary" onClick={() => void calculate(state.request)}>
                Tentar novamente
              </Button>
            </div>
          )}
        </div>

        {state.status === 'success' && (
          <>
            <LoanSummary summary={state.result.summary} />
            <h3 className="results__title">Cronograma de Amortização</h3>
            <AmortizationTable schedule={state.result.schedule} />
          </>
        )}
      </section>
    </div>
  )
}
