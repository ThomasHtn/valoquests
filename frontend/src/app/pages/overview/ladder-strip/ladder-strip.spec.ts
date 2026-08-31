import { describe, expect, it } from 'vitest';

import { ladderWindowStart } from './ladder-strip';

/*
 * `ColonyQueryService` sends one step behind the colony's own and four ahead: six while the colony is
 * anywhere on the ladder, five while it still stands on the first step and there is nothing behind.
 */
const SENT = 6;
const SENT_AT_FIRST_STEP = 5;

describe('ladderWindowStart', () => {
  it('draws everything the backend sent when there is room for all of it', () => {
    expect(ladderWindowStart(1, SENT, SENT)).toBe(0);
    expect(ladderWindowStart(0, SENT_AT_FIRST_STEP, SENT_AT_FIRST_STEP)).toBe(0);
  });

  it('keeps the colony between the step behind and the one ahead when it has to narrow', () => {
    expect(ladderWindowStart(1, SENT, 3)).toBe(0);
    expect(ladderWindowStart(3, SENT, 3)).toBe(2);
  });

  it('never runs past either end of what it was given', () => {
    expect(ladderWindowStart(0, SENT_AT_FIRST_STEP, 3)).toBe(0);
    expect(ladderWindowStart(5, SENT, 3)).toBe(3);
  });

  it('never asks for a step that was not sent', () => {
    for (const length of [SENT_AT_FIRST_STEP, SENT]) {
      for (let currentIndex = 0; currentIndex < length; currentIndex++) {
        for (const size of [3, length]) {
          const start = ladderWindowStart(currentIndex, length, size);

          expect(start).toBeGreaterThanOrEqual(0);
          expect(start + size).toBeLessThanOrEqual(length);
        }
      }
    }
  });
});
