import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../../api/httpClient'
import { API_BASE_URL } from '../../config/env'
import type { AmortizationScheduleRequest, AmortizationScheduleResponse } from './loan.types'
import { fetchAmortizationSchedule, getErrorMessage } from './loanApi'

const REQUEST: AmortizationScheduleRequest = {
  startDate: '2024-01-01',
  endDate: '2034-01-01',
  firstPaymentDate: '2024-02-15',
  principal: 140000,
  annualInterestRate: 7,
}

const RESPONSE: AmortizationScheduleResponse = {
  schedule: [],
  summary: { installmentCount: 0, totalInterest: 1, totalAmortization: 2, totalPayment: 3 },
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('fetchAmortizationSchedule', () => {
  it('posts the request as JSON to the configured endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(RESPONSE))
    vi.stubGlobal('fetch', fetchMock)

    await expect(fetchAmortizationSchedule(REQUEST)).resolves.toEqual(RESPONSE)

    expect(fetchMock).toHaveBeenCalledWith(
      `${API_BASE_URL}/api/v1/loans/amortization-schedule`,
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
      }),
    )
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual(REQUEST)
  })

  it('throws an ApiError with the status and problem body on HTTP errors', async () => {
    const problem = { status: 422, detail: 'Período inválido.' }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(problem, 422)))

    const error = await fetchAmortizationSchedule(REQUEST).catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 422, body: problem })
  })

  it('throws an ApiError without status on network failures', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    await expect(fetchAmortizationSchedule(REQUEST)).rejects.toMatchObject({ status: null })
  })

  it('rejects responses that do not match the contract', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse({ rows: [] })))
    await expect(fetchAmortizationSchedule(REQUEST)).rejects.toBeInstanceOf(ApiError)
  })
})

describe('getErrorMessage', () => {
  it('maps HTTP statuses to user-friendly messages', () => {
    expect(getErrorMessage(new ApiError('', 400))).toMatch(/parâmetros informados são inválidos/)
    expect(getErrorMessage(new ApiError('', 422))).toMatch(/não são compatíveis/)
    expect(getErrorMessage(new ApiError('', 500))).toMatch(/erro ao processar/)
    expect(getErrorMessage(new ApiError('', null))).toMatch(/conectar ao servidor/)
  })

  it('includes the backend detail for 400 and 422 responses', () => {
    expect(getErrorMessage(new ApiError('', 422, { detail: 'Período inválido.' }))).toBe(
      'Não foi possível calcular a amortização: Período inválido.',
    )
  })

  it('never exposes technical details from 500 responses', () => {
    const message = getErrorMessage(
      new ApiError('', 500, { detail: 'NullPointerException at LoanService' }),
    )
    expect(message).not.toMatch(/NullPointerException/)
  })
})
