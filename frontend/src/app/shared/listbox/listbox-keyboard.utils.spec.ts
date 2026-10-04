import { signal } from '@angular/core';
import { describe, expect, it, vi } from 'vitest';

import { ListboxKeyboardControls } from './listbox-keyboard.model';
import { handleListboxKeydown } from './listbox-keyboard.utils';

/**
 * Three-row listbox with spied actions.
 */
function setup(isOpen: boolean, activeIndex = -1): ListboxKeyboardControls {
  return {
    isOpen: signal(isOpen),
    activeIndex: signal(activeIndex),
    optionCount: 3,
    open: vi.fn(),
    close: vi.fn(),
    closeAndRefocus: vi.fn(),
    pick: vi.fn(),
  };
}

/**
 * Sends `key` and returns the event, to inspect `defaultPrevented`.
 */
function press(key: string, controls: ListboxKeyboardControls): KeyboardEvent {
  const event = new KeyboardEvent('keydown', { key, cancelable: true });
  handleListboxKeydown(event, controls);
  return event;
}

describe('handleListboxKeydown', () => {
  it.each(['ArrowDown', 'ArrowUp', 'Enter', ' '])('opens a closed list on %j', (key) => {
    const controls = setup(false);

    const event = press(key, controls);

    expect(controls.open).toHaveBeenCalledOnce();
    expect(controls.pick).not.toHaveBeenCalled();
    expect(event.defaultPrevented).toBe(true);
  });

  it('moves the highlight down and stops on the last row', () => {
    const controls = setup(true, 1);

    press('ArrowDown', controls);
    expect(controls.activeIndex()).toBe(2);

    const event = press('ArrowDown', controls);
    expect(controls.activeIndex()).toBe(2);
    expect(event.defaultPrevented).toBe(true);
    expect(controls.open).not.toHaveBeenCalled();
  });

  it('moves the highlight up and stops on the first row', () => {
    const controls = setup(true, 1);

    press('ArrowUp', controls);
    expect(controls.activeIndex()).toBe(0);

    press('ArrowUp', controls);
    expect(controls.activeIndex()).toBe(0);
  });

  it('jumps to either end with Home and End on an open list', () => {
    const controls = setup(true, 1);

    const end = press('End', controls);
    expect(controls.activeIndex()).toBe(2);
    expect(end.defaultPrevented).toBe(true);

    press('Home', controls);
    expect(controls.activeIndex()).toBe(0);
  });

  it.each(['Home', 'End'])('leaves %s alone on a closed list', (key) => {
    const controls = setup(false);

    const event = press(key, controls);

    expect(controls.activeIndex()).toBe(-1);
    expect(controls.open).not.toHaveBeenCalled();
    expect(event.defaultPrevented).toBe(false);
  });

  it.each(['Enter', ' '])('picks the highlighted row on %j', (key) => {
    const controls = setup(true, 2);

    const event = press(key, controls);

    expect(controls.pick).toHaveBeenCalledExactlyOnceWith(2);
    expect(event.defaultPrevented).toBe(true);
  });

  it('closes an open list and refocuses the trigger on Escape', () => {
    const controls = setup(true, 1);

    const event = press('Escape', controls);

    expect(controls.closeAndRefocus).toHaveBeenCalledOnce();
    expect(event.defaultPrevented).toBe(true);
  });

  it('lets Escape through on a closed list', () => {
    const controls = setup(false);

    const event = press('Escape', controls);

    expect(controls.closeAndRefocus).not.toHaveBeenCalled();
    expect(event.defaultPrevented).toBe(false);
  });

  it('closes the list on Tab without blocking focus', () => {
    const controls = setup(true, 1);

    const event = press('Tab', controls);

    expect(controls.close).toHaveBeenCalledOnce();
    expect(event.defaultPrevented).toBe(false);
  });

  it('ignores any other key', () => {
    const controls = setup(true, 1);

    const event = press('a', controls);

    expect(controls.activeIndex()).toBe(1);
    expect(controls.open).not.toHaveBeenCalled();
    expect(controls.close).not.toHaveBeenCalled();
    expect(controls.pick).not.toHaveBeenCalled();
    expect(event.defaultPrevented).toBe(false);
  });
});
