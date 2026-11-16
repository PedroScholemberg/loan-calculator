import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { API_BASE_URL } from '../../config/env'
import type { AmortizationScheduleResponse } from './loan.types'
import { LoanCalculator } from './LoanCalculator'

// Test fixture only: arbitrary numbers used to check rendering, not real results.
const RESPONSE: AmortizationScheduleResponse = {
  schedule: [
    {
      date: '2024-01-01',
      eventType: 'LOAN_START',
      installmentNumber: null,
      loanAmount: 140000,
      remainingPrincipal: 140000,
      accumulatedInterest: 0,
      paidInterest: 0,
      interestProvision: 0,
      amortization: 0,
      totalPayment: 0,
      outstandingBalance: 140000,
    },
    {
      date: '2024-01-31',
      eventType: 'MONTH_END',
      installmentNumber: null,
      loanAmount: 0,
      remainingPrincipal: 140000,
      accumulatedInterest: 0,
      paidInterest: 0,
      interestProvision: 111.11,
      amortization: 0,
      totalPayment: 0,
      outstandingBalance: 140000,
    },
    {
      date: '2024-02-15',
      eventType: 'PAYMENT',
      installmentNumber: 1,
      loanAmount: 0,
      remainingPrincipal: 139000,
      accumulatedInterest: 0,
      paidInterest: 222.22,
      interestProvision: 222.22,
      amortization: 1000,
      totalPayment: 1222.22,
      outstandingBalance: 139000,
    },
    {
      date: '2034-01-01',
      eventType: 'LOAN_END',
      installmentNumber: 2,
      loanAmount: 0,
      remainingPrincipal: 0,
      accumulatedInterest: 0,
      paidInterest: 0,
      interestProvision: 0,
      amortization: 0,
      totalPayment: 0,
      outstandingBalance: 0,
    },
  ],
  summary: { installmentCount: 2, totalInterest: 333.33, totalAmortization: 140000, totalPayment: 140333.33 },
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function mockFetch(...responses: Response[]) {
  const fetchMock = vi.fn<typeof fetch>()
  for (const response of responses) fetchMock.mockResolvedValueOnce(response)
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

const fields = {
  startDate: () => screen.getByLabelText('Data inicial'),
  endDate: () => screen.getByLabelText('Data final'),
  firstPaymentDate: () => screen.getByLabelText('Primeiro pagamento'),
  principal: () => screen.getByLabelText('Valor do empréstimo'),
  annualInterestRate: () => screen.getByLabelText('Taxa de juros anual (%)'),
}

const calculateButton = () => screen.getByRole('button', { name: /^(calcular|calculando…)$/i })

async function fillForm(
  user: ReturnType<typeof userEvent.setup>,
  values: Partial<Record<keyof typeof fields, string>> = {},
) {
  const merged = {
    startDate: '2024-01-01',
    endDate: '2034-01-01',
    firstPaymentDate: '2024-02-15',
    principal: '140.000,00',
    annualInterestRate: '7,00',
    ...values,
  }
  for (const [name, value] of Object.entries(merged)) {
    const input = fields[name as keyof typeof fields]()
    await user.clear(input)
    if (value) await user.type(input, value)
  }
}

function setup() {
  const user = userEvent.setup()
  render(<LoanCalculator />)
  return user
}

describe('LoanCalculator', () => {
  it('renders the five parameter fields and the initial instructions', () => {
    setup()

    expect(fields.startDate()).toHaveAttribute('type', 'date')
    expect(fields.endDate()).toHaveAttribute('type', 'date')
    expect(fields.firstPaymentDate()).toHaveAttribute('type', 'date')
    expect(fields.principal()).toHaveAttribute('inputmode', 'decimal')
    expect(fields.annualInterestRate()).toHaveAttribute('inputmode', 'decimal')
    expect(screen.getByRole('status')).toHaveTextContent(/preencha os parâmetros/i)
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('shows required-field errors next to the inputs and does not call the API', async () => {
    const fetchMock = mockFetch()
    const user = setup()

    await user.click(calculateButton())

    expect(fetchMock).not.toHaveBeenCalled()
    for (const field of Object.values(fields)) { await user.click(field()); await user.tab() }
    const startDate = fields.startDate()
    expect(startDate).toHaveAttribute('aria-invalid', 'true')
    expect(startDate).toHaveAccessibleDescription('Informe uma data inicial válida.')

    expect(fields.principal()).toHaveAccessibleDescription(
      expect.stringContaining('Informe o valor do empréstimo.'),
    )
    expect(fields.annualInterestRate()).toHaveAccessibleDescription(
      expect.stringContaining('Informe a taxa de juros anual.'),
    )
  })

  it('validates the order of the dates', async () => {
    const fetchMock = mockFetch()
    const user = setup()

    await fillForm(user, { endDate: '2023-12-31', firstPaymentDate: '2024-01-01' })
    await user.click(calculateButton())

    expect(fetchMock).not.toHaveBeenCalled()
    expect(fields.endDate()).toHaveAccessibleDescription(
      'A data final deve ser posterior à data inicial.',
    )
    expect(fields.firstPaymentDate()).toHaveAccessibleDescription(
      'O primeiro pagamento deve ser posterior à data inicial.',
    )

    await fillForm(user, { firstPaymentDate: '2034-02-01' })
    await user.click(calculateButton())
    expect(fields.firstPaymentDate()).toHaveAccessibleDescription(
      'O primeiro pagamento deve ser anterior à data final.',
    )
  })

  it('validates the financial values', async () => {
    const fetchMock = mockFetch()
    const user = setup()

    await fillForm(user, { principal: '0', annualInterestRate: '-1' })
    await user.click(calculateButton())

    expect(fetchMock).not.toHaveBeenCalled()
    expect(fields.principal()).toHaveAccessibleDescription(
      expect.stringContaining('O valor do empréstimo deve ser maior que zero.'),
    )
    expect(fields.annualInterestRate()).toHaveAccessibleDescription(
      expect.stringContaining('A taxa de juros não pode ser negativa.'),
    )
  })

  it.each([
    ['principal', 'Informe um valor numérico válido, por exemplo 140.000,00.'],
    ['annualInterestRate', 'Informe uma taxa numérica válida, por exemplo 7,00.'],
  ] as const)('blocks overflowing %s values and displays the field error', async (field, message) => {
    const fetchMock = mockFetch()
    const user = setup()
    await fillForm(user)
    expect(calculateButton()).toBeEnabled()

    const input = fields[field]()
    fireEvent.change(input, { target: { value: '9'.repeat(309) } })
    expect(calculateButton()).toBeDisabled()

    await user.click(input)
    await user.tab()
    expect(input).toHaveValue('9'.repeat(309))
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveAccessibleDescription(expect.stringContaining(message))

    // Submitting directly also exercises the guard independently of the disabled button.
    fireEvent.submit(input.closest('form')!)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('sends the payload with ISO dates and plain numbers', async () => {
    const fetchMock = mockFetch(jsonResponse(RESPONSE))
    const user = setup()

    await fillForm(user, { principal: '140000', annualInterestRate: '7' })
    await user.click(calculateButton())

    await screen.findByRole('table')
    expect(fetchMock).toHaveBeenCalledTimes(1)
    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe(`${API_BASE_URL}/api/v1/loans/amortization-schedule`)
    expect(init?.method).toBe('POST')
    expect(JSON.parse(String(init?.body))).toEqual({
      startDate: '2024-01-01',
      endDate: '2034-01-01',
      firstPaymentDate: '2024-02-15',
      principal: 140000,
      annualInterestRate: 7,
    })
  })

  it('formats the numeric inputs in pt-BR notation on blur', async () => {
    const user = setup()

    await user.type(fields.principal(), '140000')
    await user.type(fields.annualInterestRate(), '7')
    await user.tab()

    expect(fields.principal()).toHaveValue('140.000,00')
    expect(fields.annualInterestRate()).toHaveValue('7,00')
  })

  it('disables the button and announces progress while loading', async () => {
    let resolve: (response: Response) => void = () => {}
    vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>((r) => (resolve = r))))
    const user = setup()

    await fillForm(user)
    await user.click(calculateButton())

    expect(calculateButton()).toBeDisabled()
    expect(calculateButton()).toHaveTextContent('Calculando…')
    expect(screen.getByRole('status')).toHaveTextContent(/calculando o cronograma/i)

    resolve(jsonResponse(RESPONSE))
    await screen.findByRole('table')
    expect(calculateButton()).toBeEnabled()
  })

  it('renders the schedule in the order returned by the backend', async () => {
    mockFetch(jsonResponse(RESPONSE))
    const user = setup()

    await fillForm(user)
    await user.click(calculateButton())

    const table = await screen.findByRole('table', { name: /eventos financeiros/i })
    const headers = within(table)
      .getAllByRole('columnheader')
      .map((header) => header.textContent)
    expect(headers).toEqual([
      'Data Competência', 'Empréstimo', 'Parcela', 'Principal', 'Juros',
      'Valor de Empréstimo', 'Saldo Devedor', 'Consolidada', 'Total',
      'Amortização', 'Saldo', 'Provisão', 'Acumulado', 'Pago',
    ])
    const rows = within(table).getAllByRole('row').slice(2)
    expect(rows).toHaveLength(4)
    expect(rows.map((row) => within(row).getByRole('rowheader').textContent)).toEqual([
      '01/01/2024', '31/01/2024', '15/02/2024', '01/01/2034',
    ])
    const cells = within(rows[2]).getAllByRole('cell').map((cell) => cell.textContent?.replace(/\s/g, ' '))
    expect(cells).toEqual([
      '', 'R$ 139.000,00', '1/2', 'R$ 1.222,22', 'R$ 1.000,00',
      'R$ 139.000,00', 'R$ 222,22', 'R$ 0,00', 'R$ 222,22',
    ])
  })

  it('renders the totals returned by the backend', async () => {
    mockFetch(jsonResponse(RESPONSE))
    const user = setup()

    await fillForm(user)
    await user.click(calculateButton())

    const summary = await screen.findByRole('region', { name: 'Resumo financeiro' })
    expect(within(summary).getByText('Total de juros').nextSibling).toHaveTextContent('R$ 333,33')
    expect(within(summary).getByText('Total amortizado').nextSibling).toHaveTextContent(
      'R$ 140.000,00',
    )
    expect(within(summary).getByText('Total pago').nextSibling).toHaveTextContent(
      'R$ 140.333,33',
    )
    expect(screen.getByRole('heading', { name: 'Cronograma de Amortização' })).toBeInTheDocument()
  })

  it('shows a message when the schedule is empty', async () => {
    mockFetch(jsonResponse({ ...RESPONSE, schedule: [] }))
    const user = setup()

    await fillForm(user)
    await user.click(calculateButton())

    expect(await screen.findByText(/nenhum evento foi retornado/i)).toBeInTheDocument()
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it.each([
    [400, { detail: 'O campo principal é obrigatório.' }, 'Parâmetros inválidos: O campo principal é obrigatório.'],
    [422, {}, 'Os parâmetros informados não são compatíveis com as regras do empréstimo.'],
    [500, { detail: 'java.lang.NullPointerException' }, 'O servidor encontrou um erro ao processar a simulação.'],
  ])('shows a friendly message for HTTP %i', async (status, body, message) => {
    mockFetch(jsonResponse(body, status))
    const user = setup()

    await fillForm(user)
    await user.click(calculateButton())

    const alert = await screen.findByRole('alert')
    await waitFor(() => expect(alert).toHaveTextContent(message))
    expect(alert).not.toHaveTextContent('NullPointerException')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
  })

  it('lets the user retry after a failure', async () => {
    const fetchMock = mockFetch(jsonResponse({}, 500), jsonResponse(RESPONSE))
    const user = setup()

    await fillForm(user)
    await user.click(calculateButton())
    await user.click(await screen.findByRole('button', { name: 'Tentar novamente' }))

    expect(await screen.findByRole('table')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(fetchMock.mock.calls[1][1]?.body).toBe(fetchMock.mock.calls[0][1]?.body)
  })

  it('handles network failures', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    const user = setup()

    await fillForm(user)
    await user.click(calculateButton())

    await waitFor(() =>
      expect(screen.getByRole('alert')).toHaveTextContent(/não foi possível conectar ao servidor/i),
    )
  })

  it('hides the previous result when the parameters change', async () => {
    mockFetch(jsonResponse(RESPONSE))
    const user = setup()

    await fillForm(user)
    await user.click(calculateButton())
    await screen.findByRole('table')

    await user.type(fields.annualInterestRate(), '5')

    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    expect(screen.queryByText('Total de juros')).not.toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent(/parâmetros foram alterados/i)
  })

  it('clears the fields, errors and results', async () => {
    mockFetch(jsonResponse(RESPONSE))
    const user = setup()

    await fillForm(user)
    await user.click(calculateButton())
    await screen.findByRole('table')

    await user.click(screen.getByRole('button', { name: 'Limpar' }))

    for (const field of Object.values(fields)) expect(field()).toHaveValue('')
    expect(screen.queryByRole('table')).not.toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent(/preencha os parâmetros/i)

    await user.click(calculateButton())
    expect(fields.startDate()).toHaveAttribute('aria-invalid', 'true')

    await user.click(screen.getByRole('button', { name: 'Limpar' }))
    expect(fields.startDate()).not.toHaveAttribute('aria-invalid')
    expect(screen.queryByText('Informe uma data inicial válida.')).not.toBeInTheDocument()
  })
})
