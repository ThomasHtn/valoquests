import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { API_ENDPOINTS } from '@core/http/api-endpoints.constants';
import { PublicResources } from '@core/http/public-resources';
import { Match } from '@core/matches/match.model';
import { SquadMatch } from '@core/matches/match-squad.model';

import { SquadMatches } from './squad-matches';

/**
 * Roster match with only what the list reads.
 */
function squadMatch(id: number): SquadMatch {
  return { playerId: 1, displayName: 'natank', portrait: null, match: { id } as Match };
}

/**
 * Page body of the roster's matches.
 */
function page(index: number, ids: readonly number[], totalPages: number): object {
  return {
    content: ids.map(squadMatch),
    page: index,
    size: 20,
    totalElements: totalPages * 20,
    totalPages,
  };
}

describe('SquadMatches', () => {
  let fixture: ComponentFixture<SquadMatches>;
  let httpMock: HttpTestingController;

  /**
   * Protected state the template binds, read without rendering the history list.
   */
  const view = () =>
    fixture.componentInstance as unknown as {
      hasMore: () => boolean;
      matches: () => readonly { id: number }[];
      loadMore: () => void;
    };

  /**
   * Lets the resource take its response (a microtask away) and runs the effects.
   */
  async function settle(): Promise<void> {
    await vi.advanceTimersByTimeAsync(0);
    TestBed.tick();
  }

  /**
   * Pending request for the roster's matches at `index`.
   */
  function squadRequest(index: number) {
    return httpMock.expectOne(
      (request) =>
        request.url === API_ENDPOINTS.squadMatches && request.params.get('page') === String(index),
    );
  }

  beforeEach(async () => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    // The history list is not under test: only the paging state is.
    TestBed.overrideComponent(SquadMatches, { set: { imports: [], template: '' } });

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(SquadMatches);
    TestBed.tick();
    squadRequest(0).flush(page(0, [1, 2], 2));
    await settle();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('keeps the list and its next page when a background refresh fails', async () => {
    const publicResources = TestBed.inject(PublicResources);

    publicResources.refresh();
    TestBed.tick();
    httpMock
      .match((request) => request.url !== API_ENDPOINTS.squadMatches)
      .forEach((request) => request.flush({}));
    squadRequest(0).flush(null, { status: 503, statusText: 'Service Unavailable' });
    await settle();

    expect(
      view()
        .matches()
        .map((match) => match.id),
    ).toEqual([1, 2]);
    expect(view().hasMore()).toBe(true);
    expect(publicResources.isStale()).toBe(true);

    view().loadMore();
    TestBed.tick();
    squadRequest(1).flush(page(1, [3], 2));
    await settle();

    expect(
      view()
        .matches()
        .map((match) => match.id),
    ).toEqual([1, 2, 3]);
    expect(view().hasMore()).toBe(false);
  });

  it('follows on from the pages on screen after a failed refresh from a later page', async () => {
    view().loadMore();
    TestBed.tick();
    squadRequest(1).flush(page(1, [3], 3));
    await settle();

    TestBed.inject(PublicResources).refresh();
    TestBed.tick();
    httpMock
      .match((request) => request.url !== API_ENDPOINTS.squadMatches)
      .forEach((request) => request.flush({}));
    squadRequest(0).flush(null, { status: 503, statusText: 'Service Unavailable' });
    await settle();

    expect(
      view()
        .matches()
        .map((match) => match.id),
    ).toEqual([1, 2, 3]);

    view().loadMore();
    TestBed.tick();
    squadRequest(2).flush(page(2, [4], 3));
    await settle();

    expect(
      view()
        .matches()
        .map((match) => match.id),
    ).toEqual([1, 2, 3, 4]);
  });
});
