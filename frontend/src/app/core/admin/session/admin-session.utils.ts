import { ADMIN_API_BASE_URL } from '@core/http/api-endpoints.constants';

/**
 * Whether a request targets the administration API, so `/api/administrators` does not count.
 */
export function isAdminApiUrl(url: string): boolean {
  return url === ADMIN_API_BASE_URL || url.startsWith(`${ADMIN_API_BASE_URL}/`);
}
