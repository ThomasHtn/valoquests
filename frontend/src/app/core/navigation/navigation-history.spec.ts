import { Location } from '@angular/common';
import { provideLocationMocks } from '@angular/common/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { NavigationEnd, provideRouter, Router } from '@angular/router';

import { filter, firstValueFrom } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { NavigationHistory } from './navigation-history';

/**
 * Blank page every test route renders.
 */
@Component({ template: '' })
class Blank {}

describe('NavigationHistory', () => {
  let router: Router;
  let location: Location;
  let history: NavigationHistory;

  /**
   * Lets a navigation run to its end.
   */
  async function settle(): Promise<void> {
    await vi.advanceTimersByTimeAsync(0);
  }

  async function visit(url: string, replaceUrl = false): Promise<void> {
    await router.navigateByUrl(url, { replaceUrl });
    await settle();
  }

  /**
   * Runs a browser history step and waits for the navigation it starts to end.
   */
  async function pop(step: () => void): Promise<void> {
    const ended = firstValueFrom(
      router.events.pipe(filter((event) => event instanceof NavigationEnd)),
    );
    step();
    await vi.advanceTimersByTimeAsync(10);
    await ended;
  }

  const back = () => pop(() => location.back());
  const forward = () => pop(() => location.forward());

  beforeEach(async () => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: '', component: Blank },
          { path: 'leaderboard', component: Blank },
          { path: 'players', component: Blank },
          { path: 'players/:id', component: Blank },
          { path: 'rules', component: Blank },
        ]),
        provideLocationMocks(),
      ],
    });
    router = TestBed.inject(Router);
    location = TestBed.inject(Location);
    history = TestBed.inject(NavigationHistory);
    // Hooks the router to popstate, as bootstrapping does.
    router.initialNavigation();
    await settle();
    await visit('/leaderboard', true);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('names the page a link came from', async () => {
    await visit('/players/3');

    expect(history.previousUrl()).toBe('/leaderboard');
  });

  it('steps back through the pages visited', async () => {
    await visit('/players/3');
    await visit('/rules');
    await back();

    expect(location.path()).toBe('/players/3');
    expect(history.previousUrl()).toBe('/leaderboard');
  });

  it('follows the browser forward button too', async () => {
    await visit('/players/3');
    await back();
    expect(history.previousUrl()).toBeNull();

    await forward();

    expect(location.path()).toBe('/players/3');
    expect(history.previousUrl()).toBe('/leaderboard');
  });

  it('keeps track over several back and forward steps', async () => {
    await visit('/players/3');
    await visit('/rules');
    await back();
    await back();
    await forward();
    await forward();

    expect(location.path()).toBe('/rules');
    expect(history.previousUrl()).toBe('/players/3');

    await back();

    expect(history.previousUrl()).toBe('/leaderboard');
  });

  it('drops the forward pages once a new link is followed', async () => {
    await visit('/players');
    await visit('/players/3');
    await back();
    await visit('/rules');
    await back();
    await forward();

    expect(location.path()).toBe('/rules');
    expect(history.previousUrl()).toBe('/players');
  });

  it('rewrites the entry on a query-only change', async () => {
    await visit('/players');
    await visit('/players?sort=kda', true);
    await visit('/players/3');
    await back();

    expect(location.path()).toBe('/players?sort=kda');
    expect(history.previousUrl()).toBe('/leaderboard');
  });
});
