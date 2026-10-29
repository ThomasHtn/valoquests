import { ActivatedRouteSnapshot, UrlSegment, ViewTransitionInfo } from '@angular/router';

import { describe, expect, it, vi } from 'vitest';

import { skipSamePageTransition, snapshotPath } from './navigation-view-transition.utils';

/**
 * Snapshot chain whose levels match the given URL segments, root first.
 */
function chain(...levels: string[][]): ActivatedRouteSnapshot {
  return levels.reduceRight<ActivatedRouteSnapshot | null>(
    (child, paths) =>
      ({
        url: paths.map((path) => new UrlSegment(path, {})),
        firstChild: child,
      }) as unknown as ActivatedRouteSnapshot,
    null,
  ) as ActivatedRouteSnapshot;
}

/**
 * Transition info between two snapshots, with a spied skip.
 */
function info(from: ActivatedRouteSnapshot, to: ActivatedRouteSnapshot): ViewTransitionInfo {
  return { transition: { skipTransition: vi.fn() }, from, to } as unknown as ViewTransitionInfo;
}

describe('snapshotPath', () => {
  it('joins the segments of every active level', () => {
    expect(snapshotPath(chain([], ['players', '4'], []))).toBe('players/4');
  });
});

describe('skipSamePageTransition', () => {
  it('skips the transition when the page stays the same', () => {
    const transition = info(chain([], ['overview']), chain([], ['overview']));

    skipSamePageTransition(transition);

    expect(transition.transition.skipTransition).toHaveBeenCalled();
  });

  it('keeps the transition between two pages', () => {
    const transition = info(chain([], ['overview']), chain([], ['challenges']));

    skipSamePageTransition(transition);

    expect(transition.transition.skipTransition).not.toHaveBeenCalled();
  });
});
