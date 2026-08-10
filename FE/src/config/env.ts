const DEFAULT_API_BASE_URL = 'http://localhost:8080/api';

function normalizeBaseUrl(value: string): string {
  return value.replace(/\/+$/, '');
}

const requestDataMode = import.meta.env.VITE_REQUEST_DATA_MODE ?? 'api';

if (requestDataMode !== 'demo' && requestDataMode !== 'api') {
  throw new Error('VITE_REQUEST_DATA_MODE must be either "demo" or "api"');
}

export const env = Object.freeze({
  apiBaseUrl: normalizeBaseUrl(import.meta.env.VITE_API_BASE_URL ?? DEFAULT_API_BASE_URL),
  requestDataMode,
  isRequestDemoMode: requestDataMode === 'demo',
});
