import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { AdminSession } from './admin-session';
import { ADMIN_KEY_STORAGE_KEY } from './admin-session.constants';

describe('AdminSession', () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it('starts unauthenticated when sessionStorage holds no key', () => {
    const session = TestBed.inject(AdminSession);

    expect(session.isAuthenticated()).toBe(false);
    expect(session.key()).toBeNull();
  });

  it('starts authenticated when sessionStorage already holds a key from an earlier navigation', () => {
    sessionStorage.setItem(ADMIN_KEY_STORAGE_KEY, 'carried-over-key');

    const session = TestBed.inject(AdminSession);

    expect(session.isAuthenticated()).toBe(true);
    expect(session.key()).toBe('carried-over-key');
  });

  it('opens a session and persists the key to sessionStorage', () => {
    const session = TestBed.inject(AdminSession);

    session.signIn('accepted-key');

    expect(session.isAuthenticated()).toBe(true);
    expect(session.key()).toBe('accepted-key');
    expect(sessionStorage.getItem(ADMIN_KEY_STORAGE_KEY)).toBe('accepted-key');
  });

  it('closes a session and forgets the key from sessionStorage', () => {
    const session = TestBed.inject(AdminSession);
    session.signIn('accepted-key');

    session.signOut();

    expect(session.isAuthenticated()).toBe(false);
    expect(session.key()).toBeNull();
    expect(sessionStorage.getItem(ADMIN_KEY_STORAGE_KEY)).toBeNull();
  });
});
