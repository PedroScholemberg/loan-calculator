const DEFAULT_API_BASE_URL = 'http://localhost:8080'

/**
 * Base URL of the backend API. Configured through `VITE_API_BASE_URL`
 * (see `.env.example`); falls back to the local Spring Boot default.
 */
export const API_BASE_URL = (
  import.meta.env.VITE_API_BASE_URL?.trim() || DEFAULT_API_BASE_URL
).replace(/\/+$/, '')
