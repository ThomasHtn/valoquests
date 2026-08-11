import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  convertToParamMap,
  provideRouter,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { landingEntryGuard } from './landing-entry.guard';
import { REPLAY_QUERY_PARAM, STORAGE_KEY } from './landing-visit.constants';

describe('landingEntryGuard', () => {
  /**
   * Runs the guard against a route snapshot carrying `queryParams`, mirroring how the router
   * invokes it.
   */
  const activate = (queryParams: Record<string, string> = {}): boolean | UrlTree => {
    const route = { queryParamMap: convertToParamMap(queryParams) } as ActivatedRouteSnapshot;

    return TestBed.runInInjectionContext(
      () => landingEntryGuard(route, {} as RouterStateSnapshot) as boolean | UrlTree,
    );
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    localStorage.removeItem(STORAGE_KEY);
  });

  afterEach(() => localStorage.removeItem(STORAGE_KEY));

  it('renders the landing page on a first visit', () => {
    expect(activate()).toBe(true);
  });

  it('redirects returning visitors to the overview', () => {
    localStorage.setItem(STORAGE_KEY, 'true');

    const result = activate();

    expect(result).toBeInstanceOf(UrlTree);
    expect(String(result)).toBe('/overview');
  });

  it('re-opens the landing page for a returning visitor asking for a replay', () => {
    localStorage.setItem(STORAGE_KEY, 'true');

    expect(activate({ [REPLAY_QUERY_PARAM]: '' })).toBe(true);
  });
});
