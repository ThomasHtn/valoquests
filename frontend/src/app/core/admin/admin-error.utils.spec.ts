import { HttpErrorResponse } from '@angular/common/http';

import { resolveAdminErrorMessage, resolveSignInErrorKey } from './admin-error.utils';

describe('resolveAdminErrorMessage', () => {
  it('joins field errors before the detail', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { detail: 'Invalid request', errors: { gameName: 'must not be blank' } },
    });

    expect(resolveAdminErrorMessage(error, 'fallback')).toBe('gameName: must not be blank');
  });

  it('falls back when the response carries no detail', () => {
    expect(resolveAdminErrorMessage(new HttpErrorResponse({ status: 500 }), 'fallback')).toBe(
      'fallback',
    );
  });
});

describe('resolveSignInErrorKey', () => {
  it('words a missing or wrong key as a rejection', () => {
    expect(resolveSignInErrorKey(new HttpErrorResponse({ status: 401 }))).toBe(
      'admin.login.rejected',
    );
    expect(resolveSignInErrorKey(new HttpErrorResponse({ status: 403 }))).toBe(
      'admin.login.rejected',
    );
  });

  it('words the lockout after too many attempts', () => {
    expect(resolveSignInErrorKey(new HttpErrorResponse({ status: 429 }))).toBe(
      'admin.login.rateLimited',
    );
  });

  it('leaves other failures to the backend message', () => {
    expect(resolveSignInErrorKey(new HttpErrorResponse({ status: 500 }))).toBeNull();
    expect(resolveSignInErrorKey(new Error('offline'))).toBeNull();
  });
});
