import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { computed, Resource, ResourceRef, Signal } from '@angular/core';

/**
 * Whether any resource loads with nothing to show; a reload over a value keeps the page.
 */
export function anyLoading(...resources: readonly Resource<unknown>[]): Signal<boolean> {
  return computed(() =>
    resources.some(
      (resource) =>
        resource.status() === 'loading' ||
        (resource.status() === 'reloading' && resource.error() !== undefined),
    ),
  );
}

/**
 * Whether any resource failed, so the view shows its error rather than a partial screen.
 */
export function anyError(...resources: readonly Resource<unknown>[]): Signal<boolean> {
  return computed(() => resources.some((resource) => resource.error() !== undefined));
}

/**
 * Reloads every resource of a view, which cannot tell which one failed.
 */
export function reloadAll(...resources: readonly ResourceRef<unknown>[]): void {
  resources.forEach((resource) => resource.reload());
}

/**
 * Resource value, or `fallback` while it has none.
 * Always read through this: `value()` throws on error even with a `defaultValue`.
 */
export function resourceValue<T, F>(resource: Resource<T>, fallback: F): T | F {
  return resource.hasValue() ? resource.value() : fallback;
}

/**
 * Whether a resource failed with a 404, which no retry would fix.
 */
export function isNotFound(resource: Resource<unknown>): boolean {
  const error = resource.error();
  return error instanceof HttpErrorResponse && error.status === HttpStatusCode.NotFound;
}
