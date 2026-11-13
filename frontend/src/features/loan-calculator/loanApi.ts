import { ApiError, postJson } from '../../api/httpClient'
import type {
  AmortizationScheduleRequest,
  AmortizationScheduleResponse,
  ApiProblem,
} from './loan.types'

export const AMORTIZATION_SCHEDULE_PATH = '/api/v1/loans/amortization-schedule'

function isScheduleResponse(value: unknown): value is AmortizationScheduleResponse {
  if (typeof value !== 'object' || value === null) return false
  const { schedule, summary } = value as Record<string, unknown>
  return Array.isArray(schedule) && typeof summary === 'object' && summary !== null
}

/** Requests the amortization schedule. All financial values are computed by the backend. */
export async function fetchAmortizationSchedule(
  request: AmortizationScheduleRequest,
  signal?: AbortSignal,
): Promise<AmortizationScheduleResponse> {
  const body = await postJson(AMORTIZATION_SCHEDULE_PATH, request, signal)
  if (!isScheduleResponse(body)) {
    throw new ApiError('Unexpected response format', 200, body)
  }
  return body
}

/** Maps any error from `fetchAmortizationSchedule` to a user-facing message (pt-BR). */
export function getErrorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) {
    return 'Ocorreu um erro inesperado. Tente novamente.'
  }

  const problem = (error.body ?? {}) as ApiProblem
  const detail = typeof problem.detail === 'string' ? problem.detail.trim() : ''

  switch (error.status) {
    case null:
      return 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.'
    case 400:
      return detail
        ? `Parâmetros inválidos: ${detail}`
        : 'Os parâmetros informados são inválidos. Revise os campos e tente novamente.'
    case 422:
      return detail
        ? `Não foi possível calcular a amortização: ${detail}`
        : 'Os parâmetros informados não são compatíveis com as regras do empréstimo. Revise os valores e tente novamente.'
    case 200:
      return 'O servidor retornou uma resposta inesperada. Tente novamente mais tarde.'
    default:
      return 'O servidor encontrou um erro ao processar a simulação. Tente novamente mais tarde.'
  }
}
