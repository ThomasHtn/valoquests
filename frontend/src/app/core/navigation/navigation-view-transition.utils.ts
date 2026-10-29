import { ActivatedRouteSnapshot, ViewTransitionInfo } from '@angular/router';

/**
 * Path a route snapshot leads to, from itself down its active children, without query or fragment.
 */
export function snapshotPath(snapshot: ActivatedRouteSnapshot): string {
  const segments: string[] = [];
  for (
    let current: ActivatedRouteSnapshot | null = snapshot;
    current;
    current = current.firstChild
  ) {
    segments.push(...current.url.map((segment) => segment.path));
  }
  return segments.join('/');
}

/**
 * Skips the page transition when only the query changes, such as a tab or a filter on the same page.
 */
export function skipSamePageTransition({ transition, from, to }: ViewTransitionInfo): void {
  if (snapshotPath(from) === snapshotPath(to)) {
    transition.skipTransition();
  }
}
