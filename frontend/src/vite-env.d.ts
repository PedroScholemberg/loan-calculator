/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Base URL of the Spring Boot API, without a trailing slash. */
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
