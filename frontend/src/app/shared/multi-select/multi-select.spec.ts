import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { SelectOption } from '@shared/select/select.model';
import { MultiSelect } from './multi-select';

/**
 * Host exercising the component through the same bindings the season filter uses.
 */
@Component({
  imports: [MultiSelect],
  template: `
    <app-multi-select
      [(value)]="selected"
      [ariaLabel]="'Filter by season'"
      [maxSelection]="2"
      [maxSelectionNote]="'Two seasons at most.'"
      [options]="options()"
      [triggerLabel]="'Seasons'"
    />
  `,
})
class MultiSelectHost {
  public readonly options = signal<readonly SelectOption<number>[]>([
    { value: 1, label: 'Episode 1' },
    { value: 2, label: 'Episode 2' },
    { value: 3, label: 'Episode 3' },
  ]);
  public readonly selected = signal<readonly number[]>([1]);
}

describe('MultiSelect', () => {
  let fixture: ComponentFixture<MultiSelectHost>;
  let host: MultiSelectHost;

  /**
   * Returns the option buttons, which live under the document body once the panel has opened.
   */
  const optionButtons = (): HTMLButtonElement[] =>
    Array.from(document.body.querySelectorAll('[role="option"]'));

  /**
   * Opens the panel by clicking the trigger.
   */
  const openPanel = (): void => {
    fixture.nativeElement.querySelector('[role="combobox"], button')?.click();
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [MultiSelectHost] }).compileComponents();

    fixture = TestBed.createComponent(MultiSelectHost);
    host = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('adds a value without closing the panel, so several can be picked in one go', () => {
    openPanel();

    optionButtons()[1].click();
    fixture.detectChanges();

    expect(host.selected()).toEqual([1, 2]);
    expect(document.body.querySelector('[role="listbox"]')?.hasAttribute('hidden')).toBe(false);
  });

  it('removes a held value when it is toggled again', () => {
    host.selected.set([1, 2]);
    fixture.detectChanges();
    openPanel();

    optionButtons()[0].click();
    fixture.detectChanges();

    expect(host.selected()).toEqual([2]);
  });

  it('refuses to release the last held value, since an empty selection charts nothing', () => {
    openPanel();

    optionButtons()[0].click();
    fixture.detectChanges();

    expect(host.selected()).toEqual([1]);
  });

  it('disables the remaining options once the selection is full', () => {
    host.selected.set([1, 2]);
    fixture.detectChanges();
    openPanel();

    expect(optionButtons()[2].disabled).toBe(true);
    expect(optionButtons()[0].disabled).toBe(false);
    expect(document.body.textContent).toContain('Two seasons at most.');
  });

  it('marks the held values for assistive technology', () => {
    openPanel();

    expect(optionButtons()[0].getAttribute('aria-selected')).toBe('true');
    expect(optionButtons()[1].getAttribute('aria-selected')).toBe('false');
  });
});
