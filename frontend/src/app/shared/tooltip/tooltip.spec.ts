import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { beforeEach, describe, expect, it } from 'vitest';

import { Tooltip } from './tooltip';

/**
 * Host using the directive's real bindings.
 */
@Component({
  imports: [Tooltip],
  template: `
    <button
      [appTooltipDisabled]="disabled()"
      [appTooltip]="text()"
      appTooltipPosition="right"
      type="button"
    >
      Anchor
    </button>
  `,
})
class TooltipHost {
  public readonly text = signal('Last synchronization: 5 minutes ago');
  public readonly disabled = signal(false);
}

/**
 * Host rendering a template instead of the text.
 */
@Component({
  imports: [Tooltip],
  template: `
    <button [appTooltipTemplate]="tip" appTooltip="4 of 3" type="button">Anchor</button>
    <ng-template #tip><b class="figure">4</b> / 3</ng-template>
  `,
})
class TemplateTooltipHost {}

describe('Tooltip with a template', () => {
  it('renders the template instead of the text, and drops it on hide', () => {
    const fixture = TestBed.createComponent(TemplateTooltipHost);
    fixture.detectChanges();
    const anchor: HTMLButtonElement = fixture.nativeElement.querySelector('button');

    anchor.dispatchEvent(new MouseEvent('mouseenter'));
    const bubble = document.body.querySelector('[role="tooltip"]');
    expect(bubble?.querySelector('.figure')?.textContent).toBe('4');

    anchor.dispatchEvent(new MouseEvent('mouseleave'));
    expect(document.body.querySelector('.figure')).toBeNull();
  });
});

describe('Tooltip', () => {
  let fixture: ComponentFixture<TooltipHost>;
  let anchor: HTMLButtonElement;

  /**
   * Bubble attached to the document, if any.
   */
  const bubble = (): HTMLElement | null => document.body.querySelector('[role="tooltip"]');

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TooltipHost] }).compileComponents();

    fixture = TestBed.createComponent(TooltipHost);
    fixture.detectChanges();
    anchor = fixture.nativeElement.querySelector('button');
  });

  it('shows the bubble on hover and describes the anchor', () => {
    anchor.dispatchEvent(new MouseEvent('mouseenter'));

    expect(bubble()?.textContent).toBe('Last synchronization: 5 minutes ago');
    expect(anchor.getAttribute('aria-describedby')).toBe(bubble()?.id);
  });

  it('shows the bubble on keyboard focus, so it is reachable without a pointer', () => {
    anchor.dispatchEvent(new FocusEvent('focusin', { bubbles: true }));

    expect(bubble()).not.toBeNull();
  });

  it('removes the bubble and its reference on mouse leave', () => {
    anchor.dispatchEvent(new MouseEvent('mouseenter'));
    anchor.dispatchEvent(new MouseEvent('mouseleave'));

    expect(bubble()).toBeNull();
    expect(anchor.hasAttribute('aria-describedby')).toBe(false);
  });

  it('closes on Escape, as WAI-ARIA requires', () => {
    anchor.dispatchEvent(new MouseEvent('mouseenter'));
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(bubble()).toBeNull();
  });

  it('stays hidden while disabled', () => {
    fixture.componentInstance.disabled.set(true);
    fixture.detectChanges();

    anchor.dispatchEvent(new MouseEvent('mouseenter'));

    expect(bubble()).toBeNull();
  });

  it('stays hidden when the text is blank, rather than showing an empty bubble', () => {
    fixture.componentInstance.text.set('   ');
    fixture.detectChanges();

    anchor.dispatchEvent(new MouseEvent('mouseenter'));

    expect(bubble()).toBeNull();
  });

  it('never stacks two bubbles for repeated pointer and focus events', () => {
    anchor.dispatchEvent(new MouseEvent('mouseenter'));
    anchor.dispatchEvent(new FocusEvent('focusin', { bubbles: true }));

    expect(document.body.querySelectorAll('[role="tooltip"]')).toHaveLength(1);
  });

  it('removes a visible bubble when the anchor is destroyed', () => {
    anchor.dispatchEvent(new MouseEvent('mouseenter'));
    fixture.destroy();

    expect(bubble()).toBeNull();
  });
});
