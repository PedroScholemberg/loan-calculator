# Loan Calculator

Web application for loan amortization scheduling and interest calculation.

## Tech Stack

Java 17, Spring Boot, Maven Wrapper, React, TypeScript, Vite, Docker, and Nginx.

## Getting Started

### Docker Compose (Recommended)

Prerequisite: Docker with Compose support.

```bash
docker compose up --build
```

Application: http://localhost:8080

Stop:

```bash
docker compose down
```

### Local Development

Prerequisites: Java 17, Node.js 24, and npm. Maven Wrapper is included.
Run the backend and frontend in separate terminals.

Backend on Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Backend on Linux/macOS:

```bash
cd backend
./mvnw spring-boot:run
```

Frontend:

```bash
cd frontend
npm ci
npm run dev
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080

Vite proxies `/api/*` requests to the local backend, preserving the request path.
No environment files are required.

## API

`POST /api/v1/loans/amortization-schedule`

With Docker, use `http://localhost:8080/api/v1/loans/amortization-schedule`.
Send a JSON body with `Content-Type: application/json`:

```json
{
  "startDate": "2024-01-01",
  "endDate": "2034-01-01",
  "firstPaymentDate": "2024-02-15",
  "principal": 140000,
  "annualInterestRate": 7
}
```

`annualInterestRate` is expressed as a percentage.

## Testing

Backend:

```bash
cd backend
./mvnw clean verify
```

Windows users can use `.\mvnw.cmd clean verify`.

Frontend (after `npm ci`):

```bash
cd frontend
npm run test -- --run
npm run lint
npm run build
```

## Known Limitations

Payment dates falling on weekends are moved to the following Monday. Public holidays are not considered.
