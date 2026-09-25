import { HttpErrorResponse } from '@angular/common/http';
import { Resource, ResourceRef } from '@angular/core';
import { describe, expect, it, vi } from 'vitest';

import { anyError, anyLoading, isNotFound, reloadAll, resourceValue } from './resource-state.utils';

/**
 * Builds a minimal stand-in for a {@link Resource}, implementing only the members these utilities
 * read.
 *
 * @param options - The state the fake resource reports.
 * @returns A fake resource cast to the real type.
 */
function fakeResource<T>(options: {
  isLoading?: boolean;
  status?: string;
  error?: unknown;
  hasValue?: boolean;
  value?: T;
}): Resource<T> {
  return {
    isLoading: () => options.isLoading ?? false,
    status: () => options.status ?? (options.isLoading ? 'loading' : 'resolved'),
    error: () => options.error,
    hasValue: () => options.hasValue ?? false,
    value: () => options.value as T,
  } as unknown as Resource<T>;
}

describe('anyLoading', () => {
  it('is false when no resource is loading', () => {
    const loading = anyLoading(
      fakeResource({ isLoading: false }),
      fakeResource({ isLoading: false }),
    );

    expect(loading()).toBe(false);
  });

  it('is true as soon as one resource is loading', () => {
    const loading = anyLoading(
      fakeResource({ isLoading: false }),
      fakeResource({ isLoading: true }),
    );

    expect(loading()).toBe(true);
  });

  it('is false while a resource reloads over a value it already shows', () => {
    const loading = anyLoading(fakeResource({ isLoading: true, status: 'reloading' }));

    expect(loading()).toBe(false);
  });

  it('is true while a resource retries after an error', () => {
    const loading = anyLoading(
      fakeResource({ isLoading: true, status: 'reloading', error: new Error('boom') }),
    );

    expect(loading()).toBe(true);
  });
});

describe('anyError', () => {
  it('is false when no resource has an error', () => {
    const error = anyError(fakeResource({ error: undefined }), fakeResource({ error: undefined }));

    expect(error()).toBe(false);
  });

  it('is true as soon as one resource has an error, even if others are healthy', () => {
    const error = anyError(
      fakeResource({ error: undefined }),
      fakeResource({ error: new Error('boom') }),
    );

    expect(error()).toBe(true);
  });
});

describe('reloadAll', () => {
  it('reloads every resource given to it', () => {
    const first: ResourceRef<unknown> = { reload: vi.fn() } as unknown as ResourceRef<unknown>;
    const second: ResourceRef<unknown> = { reload: vi.fn() } as unknown as ResourceRef<unknown>;

    reloadAll(first, second);

    expect(first.reload).toHaveBeenCalledOnce();
    expect(second.reload).toHaveBeenCalledOnce();
  });
});

describe('resourceValue', () => {
  it('returns the resource value once it has one', () => {
    const resource = fakeResource({ hasValue: true, value: 'players' });

    expect(resourceValue(resource, 'fallback')).toBe('players');
  });

  it('returns the fallback while the resource has no value yet, without reading value()', () => {
    const resource = fakeResource<string>({ hasValue: false });

    expect(resourceValue(resource, 'fallback')).toBe('fallback');
  });

  it('returns the fallback for a resource stuck in error, rather than throwing', () => {
    const resource = fakeResource<string>({ hasValue: false, error: new Error('unreachable') });

    expect(resourceValue(resource, 'fallback')).toBe('fallback');
  });
});

describe('isNotFound', () => {
  it('is true for a 404 response', () => {
    expect(isNotFound(fakeResource({ error: new HttpErrorResponse({ status: 404 }) }))).toBe(true);
  });

  it('is false for any other failure or no failure at all', () => {
    expect(isNotFound(fakeResource({ error: new HttpErrorResponse({ status: 500 }) }))).toBe(false);
    expect(isNotFound(fakeResource({ error: new HttpErrorResponse({ status: 0 }) }))).toBe(false);
    expect(isNotFound(fakeResource({}))).toBe(false);
  });
});
