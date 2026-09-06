import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, it, vi } from 'vitest';

import { API_ENDPOINTS } from './api-endpoints';
import { LIVE_REFRESH_POLL_MS, LIVE_REFRESH_SETTLE_MS, LiveRefresh } from './live-refresh';

/**
 * The resources the service reloads, by the URL each one requests.
 */
const REFRESHED_URLS = [
  API_ENDPOINTS.campaign,
  API_ENDPOINTS.campaignToday,
  API_ENDPOINTS.campaignHistory,
  API_ENDPOINTS.currentRanking,
  API_ENDPOINTS.dailyRanking,
  API_ENDPOINTS.currentChallenges,
];

function roster(lastSuccessfulSynchronizationAt: string): unknown[] {
  return [{ id: 1, displayName: 'Op', lastSuccessfulSynchronizationAt }];
}

describe('LiveRefresh', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 8, 7, 12, 0, 0));

    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    httpMock = TestBed.inject(HttpTestingController);
    TestBed.inject(LiveRefresh);
    await settle();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  /**
   * Lets the resources take their responses in (a microtask away) and runs the effects.
   */
  async function settle(): Promise<void> {
    await vi.advanceTimersByTimeAsync(0);
    TestBed.tick();
  }

  /**
   * Answers every request the shared resources fired on creation, and the roster with `syncedAt`.
   */
  async function settleInitialLoad(syncedAt: string): Promise<void> {
    httpMock.expectOne(API_ENDPOINTS.players).flush(roster(syncedAt));
    httpMock.match(() => true).forEach((request) => request.flush({}));
    await settle();
  }

  async function pollRoster(syncedAt: string): Promise<void> {
    await vi.advanceTimersByTimeAsync(LIVE_REFRESH_POLL_MS);
    TestBed.tick();
    httpMock.expectOne(API_ENDPOINTS.players).flush(roster(syncedAt));
    await settle();
  }

  async function letTheReloadSettle(): Promise<void> {
    await vi.advanceTimersByTimeAsync(LIVE_REFRESH_SETTLE_MS);
    TestBed.tick();
  }

  function expectScreensReloaded(): void {
    REFRESHED_URLS.forEach((url) => httpMock.expectOne(url));
    httpMock
      .match((request) => request.url === API_ENDPOINTS.rankingHistory)
      .forEach((request) => request.flush({}));
    httpMock.verify();
  }

  it('reloads the screens once a later synchronization shows up on the roster', async () => {
    await settleInitialLoad('2026-09-07T10:00:00Z');

    await pollRoster('2026-09-07T10:30:00Z');
    httpMock.expectNone(API_ENDPOINTS.campaign);

    await letTheReloadSettle();

    expectScreensReloaded();
  });

  it('leaves the screens alone while the roster reports the same synchronization', async () => {
    await settleInitialLoad('2026-09-07T10:00:00Z');

    await pollRoster('2026-09-07T10:00:00Z');
    await letTheReloadSettle();

    httpMock.verify();
  });

  it('reloads the screens once the day has turned, even without a synchronization', async () => {
    await settleInitialLoad('2026-09-07T10:00:00Z');

    vi.setSystemTime(new Date(2026, 8, 8, 0, 20, 0));
    await pollRoster('2026-09-07T10:00:00Z');
    await letTheReloadSettle();

    expectScreensReloaded();
  });

  it('polls the roster at once when the tab comes back to the foreground', async () => {
    await settleInitialLoad('2026-09-07T10:00:00Z');

    document.dispatchEvent(new Event('visibilitychange'));
    await settle();

    httpMock.expectOne(API_ENDPOINTS.players).flush(roster('2026-09-07T10:00:00Z'));
    httpMock.verify();
  });
});
