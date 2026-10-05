import { afterEach, describe, expect, it, vi } from 'vitest';

import { readStorage, removeStorage, writeStorage } from './safe-storage.utils';

describe('safe storage', () => {
  afterEach(() => vi.restoreAllMocks());

  it('reads back what it wrote, then forgets what it removed', () => {
    writeStorage('valo-quests.test', 'x');
    expect(readStorage('valo-quests.test')).toBe('x');
    removeStorage('valo-quests.test');
    expect(readStorage('valo-quests.test')).toBeNull();
  });

  it('keeps session values apart from local ones', () => {
    writeStorage('valo-quests.test', 'tab', 'session');
    expect(sessionStorage.getItem('valo-quests.test')).toBe('tab');
    expect(readStorage('valo-quests.test')).toBeNull();
    removeStorage('valo-quests.test', 'session');
    expect(readStorage('valo-quests.test', 'session')).toBeNull();
  });

  it('survives a storage that throws', () => {
    for (const method of ['getItem', 'setItem', 'removeItem'] as const) {
      vi.spyOn(Storage.prototype, method).mockImplementation(() => {
        throw new DOMException('blocked', 'SecurityError');
      });
    }
    expect(readStorage('any')).toBeNull();
    expect(() => writeStorage('any', 'x')).not.toThrow();
    expect(() => removeStorage('any', 'session')).not.toThrow();
  });
});
