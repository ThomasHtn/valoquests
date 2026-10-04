import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '@env/environment';

import { adminKeyInterceptor } from './admin-key.interceptor';
import { AdminSession } from './admin-session';
import { ADMIN_KEY_HEADER, ADMIN_LOGIN_ROUTE } from './admin-session.constants';

const ADMIN_URL = `${environment.apiBaseUrl}/admin/players`;
const PUBLIC_URL = `${environment.apiBaseUrl}/players`;

describe('adminKeyInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let session: AdminSession;
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    sessionStorage.clear();
    navigate = vi.fn();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([adminKeyInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigate } },
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    session = TestBed.inject(AdminSession);
  });

  it('leaves a public API request untouched, even with an open session', () => {
    session.signIn('held-key');

    httpClient.get(PUBLIC_URL).subscribe();

    const request = httpMock.expectOne(PUBLIC_URL);
    expect(request.request.headers.has(ADMIN_KEY_HEADER)).toBe(false);
    request.flush({});
  });

  it('attaches the held key to an administration request', () => {
    session.signIn('held-key');

    httpClient.get(ADMIN_URL).subscribe();

    const request = httpMock.expectOne(ADMIN_URL);
    expect(request.request.headers.get(ADMIN_KEY_HEADER)).toBe('held-key');
    request.flush({});
  });

  it('sends an administration request without the header when no session is open', () => {
    httpClient.get(ADMIN_URL).subscribe();

    const request = httpMock.expectOne(ADMIN_URL);
    expect(request.request.headers.has(ADMIN_KEY_HEADER)).toBe(false);
    request.flush({});
  });

  it('never overwrites a header a caller already attached, as the sign-in probe does', () => {
    session.signIn('stale-key');

    httpClient.get(ADMIN_URL, { headers: { [ADMIN_KEY_HEADER]: 'candidate-key' } }).subscribe();

    const request = httpMock.expectOne(ADMIN_URL);
    expect(request.request.headers.get(ADMIN_KEY_HEADER)).toBe('candidate-key');
    request.flush({});
  });

  it('signs out and redirects to login when an authenticated request is rejected as unauthorized', () => {
    session.signIn('revoked-key');

    httpClient.get(ADMIN_URL).subscribe({ error: () => undefined });

    httpMock.expectOne(ADMIN_URL).flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(session.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith([ADMIN_LOGIN_ROUTE]);
  });

  it('signs out and redirects to login when an authenticated request is rejected as forbidden', () => {
    session.signIn('revoked-key');

    httpClient.get(ADMIN_URL).subscribe({ error: () => undefined });

    httpMock.expectOne(ADMIN_URL).flush(null, { status: 403, statusText: 'Forbidden' });

    expect(session.isAuthenticated()).toBe(false);
    expect(navigate).toHaveBeenCalledWith([ADMIN_LOGIN_ROUTE]);
  });

  it('never signs out over a rejected sign-in probe, which carries its own header', () => {
    session.signIn('unrelated-held-key');

    httpClient
      .get(ADMIN_URL, { headers: { [ADMIN_KEY_HEADER]: 'candidate-key' } })
      .subscribe({ error: () => undefined });

    httpMock.expectOne(ADMIN_URL).flush(null, { status: 403, statusText: 'Forbidden' });

    expect(session.isAuthenticated()).toBe(true);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('leaves an unrelated error status alone, without signing out', () => {
    session.signIn('held-key');

    httpClient.get(ADMIN_URL).subscribe({ error: () => undefined });

    httpMock.expectOne(ADMIN_URL).flush(null, { status: 500, statusText: 'Internal Server Error' });

    expect(session.isAuthenticated()).toBe(true);
    expect(navigate).not.toHaveBeenCalled();
  });
});
