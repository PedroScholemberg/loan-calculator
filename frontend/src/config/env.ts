/** Optional API prefix; the default uses the current origin through Vite or Nginx. */
export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL?.trim() || '').replace(/\/+$/, '')
