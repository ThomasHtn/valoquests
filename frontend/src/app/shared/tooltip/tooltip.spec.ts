import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { Tooltip } from './tooltip';

/**
 * Host exercising the directive through the same bindings the application uses.
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

describe('Tooltip', () => {
  let fixture: ComponentFixture<TooltipHost>;
  let anchor: HTMLButtonElement;

  /**
   * Returns the bubble currently attached to the document, if any.
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
