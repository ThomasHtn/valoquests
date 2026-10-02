import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { Season } from '@core/matches/season.model';
import { SeasonPicker } from './season-picker';

/**
 * Three acts, newest first, as the profile passes them.
 */
const SEASONS: readonly Season[] = [
  { id: 3, name: 'e11a3', active: true },
  { id: 2, name: 'e11a2', active: false },
  { id: 1, name: 'e11a1', active: false },
];

/**
 * Host binding one single-select and one multi-select picker, like the profile's two views.
 */
@Component({
  imports: [SeasonPicker],
  template: `
    <app-season-picker
      [(selection)]="single"
      [ariaLabel]="'Season'"
      [seasons]="seasons"
      class="single"
    />
    <app-season-picker
      [(selection)]="several"
      [ariaLabel]="'Seasons'"
      [maxSelection]="2"
      [multiple]="true"
      [seasons]="seasons"
      class="several"
    />
  `,
})
class SeasonPickerHost {
  public readonly seasons = SEASONS;
  public readonly single = signal<readonly number[]>([]);
  public readonly several = signal<readonly number[]>([3]);
}

describe('SeasonPicker', () => {
  let fixture: ComponentFixture<SeasonPickerHost>;
  let host: SeasonPickerHost;

  const trigger = (scope: string): HTMLButtonElement =>
    fixture.nativeElement.querySelector(`.${scope} [role="combobox"]`);
  const listbox = (scope: string): HTMLElement =>
    fixture.nativeElement.querySelector(`.${scope} [role="listbox"]`);
  const options = (scope: string): HTMLElement[] =>
    Array.from(fixture.nativeElement.querySelectorAll(`.${scope} [role="option"]`));

  /**
   * Presses a key on a picker's trigger and lets the view catch up.
   */
  const press = (scope: string, key: string): void => {
    trigger(scope).dispatchEvent(
      new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true }),
    );
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SeasonPickerHost],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(SeasonPickerHost);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('opens on the "every season" row when nothing is picked, and steps into the acts', () => {
    press('single', 'ArrowDown');
    expect(trigger('single').getAttribute('aria-activedescendant')).toBe(options('single')[0].id);

    press('single', 'ArrowDown');
    press('single', 'ArrowDown');
    press('single', 'Enter');

    expect(host.single()).toEqual([2]);
    expect(listbox('single').hidden).toBe(true);
  });

  it('goes back to every season from the first row', () => {
    host.single.set([1]);
    fixture.detectChanges();

    press('single', 'ArrowDown');
    expect(trigger('single').getAttribute('aria-activedescendant')).toBe(options('single')[3].id);

    press('single', 'Home');
    press('single', ' ');
    expect(host.single()).toEqual([]);
  });

  it('toggles the highlighted act in multiple mode and keeps the list open', () => {
    trigger('several').focus();
    press('several', 'ArrowDown');
    press('several', 'ArrowDown');
    press('several', ' ');

    expect(host.several()).toEqual([3, 2]);
    expect(listbox('several').hidden).toBe(false);

    press('several', 'Enter');
    expect(host.several()).toEqual([3]);
    expect(listbox('several').hidden).toBe(false);

    press('several', 'Escape');
    expect(listbox('several').hidden).toBe(true);
    expect(document.activeElement).toBe(trigger('several'));
  });

  it('leaves an act greyed out by the limit alone', () => {
    host.several.set([3, 2]);
    fixture.detectChanges();

    press('several', 'ArrowDown');
    press('several', 'End');
    press('several', 'Enter');

    expect(host.several()).toEqual([3, 2]);
  });

  it('closes on Tab', () => {
    press('several', 'ArrowDown');
    press('several', 'Tab');

    expect(listbox('several').hidden).toBe(true);
  });
});
