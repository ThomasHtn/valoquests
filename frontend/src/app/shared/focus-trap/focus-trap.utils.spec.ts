import { describe, expect, it } from 'vitest';

import { focusableWithin } from './focus-trap.utils';

describe('focusableWithin', () => {
  it('leaves out disabled controls and inert branches', () => {
    const root = document.createElement('div');
    root.innerHTML = `
      <button id="a">A</button>
      <button disabled>B</button>
      <div inert><button>C</button></div>
      <span tabindex="0" id="d">D</span>
      <span tabindex="-1">E</span>`;
    document.body.append(root);
    // jsdom lays nothing out, so every element reports no box: stub one for the visible ones.
    for (const element of root.querySelectorAll<HTMLElement>('#a, #d')) {
      element.getClientRects = () => [{}] as unknown as DOMRectList;
    }
    expect(focusableWithin(root).map((element) => element.id)).toEqual(['a', 'd']);
    root.remove();
  });
});
