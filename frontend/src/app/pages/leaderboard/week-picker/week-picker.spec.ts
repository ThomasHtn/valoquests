import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it } from 'vitest';

import { WeekOption } from '../leaderboard.model';
import { WeekPicker } from './week-picker';

/**
 * Week outside any campaign, enough for the list to render.
 */
function week(weekStart: string): WeekOption {
  return { weekStart, label: weekStart, index: null, group: null, live: false, winner: null };
}

/**
 * Host binding the picker like the leaderboard.
 */
@Component({
  imports: [WeekPicker],
  template: `
    <app-week-picker
      (selectedChange)="selected.set($event)"
      [options]="options"
      [selected]="selected()"
    />
  `,
})
class WeekPickerHost {
  public readonly options = [week('2026-09-28'), week('2026-09-21'), week('2026-09-14')];
  public readonly selected = signal<string | null>('2026-09-21');
}

describe('WeekPicker', () => {
  let fixture: ComponentFixture<WeekPickerHost>;
  let host: WeekPickerHost;

  const trigger = (): HTMLButtonElement => fixture.nativeElement.querySelector('[role="combobox"]');
  const listbox = (): HTMLElement => fixture.nativeElement.querySelector('[role="listbox"]');
  const options = (): HTMLElement[] =>
    Array.from(fixture.nativeElement.querySelectorAll('[role="option"]'));

  /**
   * Presses a key on the trigger and detects changes.
   */
  const press = (key: string): KeyboardEvent => {
    const event = new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true });
    trigger().dispatchEvent(event);
    fixture.detectChanges();
    return event;
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WeekPickerHost],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(WeekPickerHost);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('opens on ArrowDown with the week on screen highlighted', () => {
    press('ArrowDown');

    expect(listbox().hidden).toBe(false);
    expect(trigger().getAttribute('aria-activedescendant')).toBe(options()[1].id);
    expect(options()[1].hasAttribute('data-active')).toBe(true);
  });

  it('moves the highlight with the arrows, Home and End, clamped at both ends', () => {
    press('ArrowDown');
    press('ArrowDown');
    press('ArrowDown');
    expect(trigger().getAttribute('aria-activedescendant')).toBe(options()[2].id);

    press('Home');
    expect(trigger().getAttribute('aria-activedescendant')).toBe(options()[0].id);
    press('ArrowUp');
    expect(trigger().getAttribute('aria-activedescendant')).toBe(options()[0].id);

    press('End');
    expect(trigger().getAttribute('aria-activedescendant')).toBe(options()[2].id);
  });

  it('picks the highlighted week on Enter, closes and keeps focus on the trigger', () => {
    trigger().focus();
    press('Enter');
    press('ArrowUp');
    const event = press('Enter');

    expect(event.defaultPrevented).toBe(true);
    expect(host.selected()).toBe('2026-09-28');
    expect(listbox().hidden).toBe(true);
    expect(document.activeElement).toBe(trigger());
  });

  it('closes on Escape without picking, and on Tab', () => {
    press(' ');
    press('ArrowDown');
    press('Escape');

    expect(listbox().hidden).toBe(true);
    expect(host.selected()).toBe('2026-09-21');
    expect(trigger().hasAttribute('aria-activedescendant')).toBe(false);

    press('ArrowDown');
    press('Tab');
    expect(listbox().hidden).toBe(true);
  });

  it('picks a tapped week and hands focus back to the trigger', () => {
    trigger().click();
    fixture.detectChanges();
    options()[2].focus();

    options()[2].click();
    fixture.detectChanges();

    expect(host.selected()).toBe('2026-09-14');
    expect(document.activeElement).toBe(trigger());
  });
});
