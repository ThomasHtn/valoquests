import { computed, Resource, ResourceRef, Signal } from '@angular/core';

/**
 * Combines the loading state of several resources a single view depends on.
 *
 * @param resources - The resources backing the view.
 * @returns Whether at least one of them is still loading.
 */
export function anyLoading(...resources: readonly Resource<unknown>[]): Signal<boolean> {
  return computed(() => resources.some((resource) => resource.isLoading()));
}

/**
 * Combines the error state of several resources a single view depends on.
 *
 * A view is only as healthy as its least healthy dependency, so one failed request is enough to
 * show the error state rather than a partially populated screen.
 *
 * @param resources - The resources backing the view.
 * @returns Whether at least one of them failed to load.
 */
export function anyError(...resources: readonly Resource<unknown>[]): Signal<boolean> {
  return computed(() => resources.some((resource) => resource.error() !== undefined));
}

/**
 * Reloads every resource a single view depends on.
 *
 * Counterpart to {@link anyLoading} and {@link anyError}: a view that reports the combined state of
 * several resources must also retry all of them, since it cannot tell which one failed.
 *
 * @param resources - The resources backing the view.
 */
export function reloadAll(...resources: readonly ResourceRef<unknown>[]): void {
  resources.forEach((resource) => resource.reload());
}

/**
 * Reads a resource's current value, without throwing while it is loading or has failed.
 *
 * `Resource.value()` throws once a resource settles into an error state (e.g. the backend is
 * unreachable), even for resources declared with a `defaultValue`. Every computed or template
 * expression that derives from a resource's value must go through this guard instead of calling
 * `value()` directly, so a failed request degrades to `fallback` rather than breaking navigation.
 *
 * @param resource - The resource to read.
 * @param fallback - The value to use while the resource has no defined value yet.
 * @returns The resource's current value, or `fallback`.
 */
export function resourceValue<T, F>(resource: Resource<T>, fallback: F): T | F {
  return resource.hasValue() ? resource.value() : fallback;
}
