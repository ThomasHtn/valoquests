import { describe, expect, it } from 'vitest';

import { isStaleChunkError } from './navigation-stale-chunk.utils';

describe('isStaleChunkError', () => {
  it('recognises a lazy chunk that failed to load', () => {
    expect(
      isStaleChunkError(new TypeError('Failed to fetch dynamically imported module: /chunk-A.js')),
    ).toBe(true);
    expect(isStaleChunkError(new TypeError('Importing a module script failed.'))).toBe(true);
  });

  it('leaves any other navigation error alone', () => {
    expect(isStaleChunkError(new Error('Cannot match any routes'))).toBe(false);
    expect(isStaleChunkError(undefined)).toBe(false);
  });
});
