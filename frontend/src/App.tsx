import { LoanCalculator } from './features/loan-calculator/LoanCalculator'

function App() {
  return (
    <div className="app">
      <header className="app__header">
        <h1 className="app__title">Calculadora de Empréstimos</h1>
        <p className="app__subtitle">
          Simule a evolução de um empréstimo e visualize o cronograma de pagamentos,
          juros e amortizações.
        </p>
      </header>
      <main>
        <LoanCalculator />
      </main>
    </div>
  )
}

export default App
