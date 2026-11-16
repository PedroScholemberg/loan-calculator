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

- Application: http://localhost:3000
- Backend API: http://localhost:8081
- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI JSON: http://localhost:8081/v3/api-docs

Docker publishes backend port 8080 on host port 8081, while the frontend uses host port 3000.

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
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Vite proxies `/api/*` requests to the local backend, preserving the request path.
No environment files are required.

## API

`POST /api/v1/loans/amortization-schedule`

With Docker, call the backend directly at `http://localhost:8081/api/v1/loans/amortization-schedule`
or through the frontend proxy at `http://localhost:3000/api/v1/loans/amortization-schedule`.
Swagger UI documents the request, response schemas, and errors, and lets you try the endpoint.
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

## Documentation

- [Architecture](docs/architecture.md)
- [Business rules](docs/business-rules.md)
- [Technical decisions](docs/technical-decisions.md)
- [Testing strategy](docs/testing.md)

## Known Limitations

Monthly payment dates falling on weekends are moved to the following Monday. The mandatory final payment remains on the end date. Public holidays are not considered.
