import { afterEach, describe, expect, it, vi } from 'vitest';

import { readStorage, writeStorage } from './safe-storage.utils';

describe('safe storage', () => {
  afterEach(() => vi.restoreAllMocks());

  it('reads back what it wrote', () => {
    writeStorage('valo-quests.test', 'x');
    expect(readStorage('valo-quests.test')).toBe('x');
    localStorage.removeItem('valo-quests.test');
  });

  it('survives a storage that throws', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError');
    });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new DOMException('blocked', 'SecurityError');
    });
    expect(readStorage('any')).toBeNull();
    expect(() => writeStorage('any', 'x')).not.toThrow();
  });
});
