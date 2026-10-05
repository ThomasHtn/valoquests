import { describe, expect, it } from 'vitest';

import { ADMIN_API_BASE_URL } from '@core/http/api-endpoints.constants';
import { environment } from '@env/environment';

import { isAdminApiUrl } from './admin-session.utils';

describe('isAdminApiUrl', () => {
  it('matches the administration root and every route under it', () => {
    expect(isAdminApiUrl(ADMIN_API_BASE_URL)).toBe(true);
    expect(isAdminApiUrl(`${ADMIN_API_BASE_URL}/players`)).toBe(true);
  });

  it('ignores a public route that merely starts with the same letters', () => {
    expect(isAdminApiUrl(`${environment.apiBaseUrl}/administrators`)).toBe(false);
    expect(isAdminApiUrl(`${environment.apiBaseUrl}/players`)).toBe(false);
  });
});
