import { describe, expect, it } from 'vitest';

import { parseRouteId } from './route-id.utils';

describe('parseRouteId', () => {
  it('reads a positive integer', () => {
    expect(parseRouteId('42')).toBe(42);
  });

  it.each(['abc', '', '0', '-1', '1.5', '1e3', ' 7', '99999999999999999999'])(
    'rejects %j',
    (raw) => {
      expect(parseRouteId(raw)).toBeNull();
    },
  );
});
