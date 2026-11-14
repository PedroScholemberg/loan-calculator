import { API_BASE_URL } from '../config/env'

/**
 * Raised for any failed API call. `status` is the HTTP status code, or
 * `null` when no response was received (network error, CORS, etc.).
 */
export class ApiError extends Error {
  readonly status: number | null
  readonly body: unknown

  constructor(message: string, status: number | null, body?: unknown) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.body = body
  }
}

async function readJson(response: Response): Promise<unknown> {
  const text = await response.text()
  if (!text) return undefined
  try {
    return JSON.parse(text)
  } catch {
    return undefined
  }
}

/** Sends a JSON `POST` to `path` (relative to `API_BASE_URL`) and returns the parsed body. */
export async function postJson(
  path: string,
  payload: unknown,
  signal?: AbortSignal,
): Promise<unknown> {
  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify(payload),
      signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new ApiError('Network request failed', null)
  }

  const body = await readJson(response)
  if (!response.ok) {
    throw new ApiError(`Request failed with status ${response.status}`, response.status, body)
  }
  return body
}
