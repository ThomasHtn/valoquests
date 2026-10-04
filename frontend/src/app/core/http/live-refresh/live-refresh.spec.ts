import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, it, vi } from 'vitest';

import { API_ENDPOINTS } from '../api-endpoints.constants';
import { LiveRefresh } from './live-refresh';
import { LIVE_REFRESH_POLL_MS } from './live-refresh.constants';

/**
 * URLs of the resources the service reloads.
 */
const REFRESHED_URLS = [
  API_ENDPOINTS.campaign,
  API_ENDPOINTS.campaignToday,
  API_ENDPOINTS.campaignHistory,
  API_ENDPOINTS.currentRanking,
  API_ENDPOINTS.dailyRanking,
  API_ENDPOINTS.currentChallenges,
];

function status(lastCompletedAt: string, inProgress = false): object {
  return { inProgress, lastCompletedAt };
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
   * Lets the resources take their responses (a microtask away) and runs the effects.
   */
  async function settle(): Promise<void> {
    await vi.advanceTimersByTimeAsync(0);
    TestBed.tick();
  }

  /**
   * Answers the initial requests, the status finished at `at`.
   */
  async function settleInitialLoad(at: string): Promise<void> {
    httpMock.expectOne(API_ENDPOINTS.synchronizationStatus).flush(status(at));
    httpMock.match(() => true).forEach((request) => request.flush({}));
    await settle();
  }

  async function pollStatus(at: string, inProgress = false): Promise<void> {
    await vi.advanceTimersByTimeAsync(LIVE_REFRESH_POLL_MS);
    TestBed.tick();
    httpMock.expectOne(API_ENDPOINTS.synchronizationStatus).flush(status(at, inProgress));
    await settle();
  }

  function expectScreensReloaded(): void {
    REFRESHED_URLS.forEach((url) => httpMock.expectOne(url));
    httpMock
      .match((request) =>
        (
          [
            API_ENDPOINTS.rankingHistory,
            API_ENDPOINTS.players,
            API_ENDPOINTS.synchronizationStatus,
          ] as string[]
        ).includes(request.url),
      )
      .forEach((request) => request.flush({}));
    httpMock.verify();
  }

  it('reloads the screens as soon as a later synchronization has finished', async () => {
    await settleInitialLoad('2026-09-07T10:00:00Z');

    await pollStatus('2026-09-07T10:30:00Z');

    expectScreensReloaded();
  });

  it('leaves the screens alone while a synchronization is still running', async () => {
    await settleInitialLoad('2026-09-07T10:00:00Z');

    await pollStatus('2026-09-07T10:00:00Z', true);
    await pollStatus('2026-09-07T10:00:00Z', true);

    httpMock.verify();
  });

  it('leaves the screens alone while the status reports the same synchronization', async () => {
    await settleInitialLoad('2026-09-07T10:00:00Z');

    await pollStatus('2026-09-07T10:00:00Z');

    httpMock.verify();
  });

  it('reloads the screens once the day has turned, even without a synchronization', async () => {
    await settleInitialLoad('2026-09-07T10:00:00Z');

    vi.setSystemTime(new Date(2026, 8, 8, 0, 20, 0));
    await pollStatus('2026-09-07T10:00:00Z');

    expectScreensReloaded();
  });

  it('polls the status at once when the tab comes back to the foreground', async () => {
    await settleInitialLoad('2026-09-07T10:00:00Z');

    document.dispatchEvent(new Event('visibilitychange'));
    await settle();

    httpMock.expectOne(API_ENDPOINTS.synchronizationStatus).flush(status('2026-09-07T10:00:00Z'));
    httpMock.verify();
  });
});
