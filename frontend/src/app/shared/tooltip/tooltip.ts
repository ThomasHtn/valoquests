import { Directive, ElementRef, OnDestroy, Renderer2, inject, input, signal } from '@angular/core';

import { TOOLTIP_SURFACE_CLASS } from './tooltip.constants';

/**
 * Monotonically increasing counter backing {@link Tooltip.tooltipId}.
 *
 * A per-instance id is required because the host references its tooltip through
 * `aria-describedby`, which must resolve to exactly one element in the document.
 */
let instanceCount = 0;

/**
 * Distance in pixels between the host element and its tooltip.
 */
const OFFSET = 8;

/**
 * Side of the host the tooltip is rendered on.
 */
export type TooltipPosition = 'above' | 'below' | 'left' | 'right';

/**
 * Shows a short text bubble describing its host on hover and on keyboard focus.
 *
 * Replaces Angular Material's `matTooltip`, which pulled `@angular/material` and `@angular/cdk`
 * into the initial bundle for this single feature. Every tooltip in this application supplements
 * information already available elsewhere (a visible label, an `aria-label` or an `sr-only` span),
 * so the bubble is an enhancement rather than the only way to read the interface.
 *
 * The bubble is a native popover, which renders in the browser's top layer. That is what makes it
 * immune to being clipped by a scrolling or `overflow: hidden` ancestor, such as the collapsed
 * sidebar rail, and it is the reason an absolutely positioned element is not enough here.
 *
 * Accessibility: the bubble carries `role="tooltip"` and is referenced by `aria-describedby` while
 * visible, it opens on focus as well as on hover so it is reachable without a pointer, and it
 * closes on Escape as WAI-ARIA requires.
 */
@Directive({
  selector: '[appTooltip]',
  host: {
    '(mouseenter)': 'show()',
    '(mouseleave)': 'hide()',
    '(focusin)': 'show()',
    '(focusout)': 'hide()',
  },
})
export class Tooltip implements OnDestroy {
  /**
   * Already-translated text rendered inside the bubble.
   */
  public readonly appTooltip = input.required<string>();

  /**
   * Side of the host the bubble is rendered on.
   */
  public readonly appTooltipPosition = input<TooltipPosition>('above');

  /**
   * Suppresses the tooltip without removing the directive.
   *
   * Used by the sidebar, whose navigation entries only need a tooltip while the rail is collapsed
   * and hides their labels.
   */
  public readonly appTooltipDisabled = input(false);

  /**
   * Bubble size. `md` is used by the sidebar's navigation entries, whose tooltip is the only way to
   * read the entry's label while the rail is collapsed and therefore needs to read comfortably at a
   * glance; every other tooltip stays at the default `sm`.
   *
   * Both steps sit a size above the surrounding micro-labels rather than below them: a bubble
   * explaining how a figure is worked out is a sentence to be read, not a caption to be scanned,
   * and at the caption's own size it was the hardest text on the page to make out.
   */
  public readonly appTooltipSize = input<'sm' | 'md'>('sm');

  /**
   * Host element the bubble is positioned against and described by.
   */
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /**
   * Renderer used to create and mutate the bubble outside the component template.
   */
  private readonly renderer = inject(Renderer2);

  /**
   * Unique identifier linking the host to its bubble through `aria-describedby`.
   */
  private readonly tooltipId = `app-tooltip-${(instanceCount += 1)}`;

  /**
   * Bubble currently attached to the document, or `null` while hidden.
   */
  private readonly bubble = signal<HTMLElement | null>(null);

  /**
   * Removes the document-level Escape listener, or `null` while hidden.
   */
  private escapeListener: (() => void) | null = null;

  /**
   * Removes the bubble when the host is destroyed while its tooltip is visible.
   *
   * A popover lives in the top layer attached to the document body, so it would otherwise outlive
   * the element it describes, for instance when a route change removes the host under the pointer.
   */
  public ngOnDestroy(): void {
    this.hide();
  }

  /**
   * Creates, positions and reveals the bubble.
   *
   * Does nothing when the tooltip is disabled, when its text is blank or when a bubble is already
   * visible, so repeated pointer and focus events cannot stack several bubbles.
   */
  protected show(): void {
    if (this.appTooltipDisabled() || !this.appTooltip().trim() || this.bubble()) {
      return;
    }

    const bubble = this.renderer.createElement('div') as HTMLElement;
    this.renderer.setAttribute(bubble, 'id', this.tooltipId);
    this.renderer.setAttribute(bubble, 'role', 'tooltip');
    this.renderer.setAttribute(bubble, 'popover', 'manual');
    this.renderer.setProperty(bubble, 'textContent', this.appTooltip());
    const sizeClass =
      this.appTooltipSize() === 'md'
        ? 'max-w-80 px-4 py-3 text-base'
        : 'max-w-72 px-3 py-2 text-sm';
    this.renderer.setAttribute(
      bubble,
      'class',
      `${TOOLTIP_SURFACE_CLASS} pointer-events-none m-0 ${sizeClass}`,
    );
    this.renderer.setStyle(bubble, 'position', 'fixed');

    this.renderer.appendChild(this.document().body, bubble);
    this.togglePopover(bubble, true);

    this.position(bubble);
    this.renderer.setAttribute(this.host.nativeElement, 'aria-describedby', this.tooltipId);
    this.bubble.set(bubble);
    this.listenForEscape();
  }

  /**
   * Removes the bubble and every reference to it.
   */
  protected hide(): void {
    const bubble = this.bubble();
    if (!bubble) {
      return;
    }

    this.escapeListener?.();
    this.escapeListener = null;

    this.renderer.removeAttribute(this.host.nativeElement, 'aria-describedby');
    this.togglePopover(bubble, false);
    this.renderer.removeChild(this.document().body, bubble);
    this.bubble.set(null);
  }

  /**
   * Promotes the bubble to the top layer, or leaves it in flow where popovers are unavailable.
   *
   * The bubble is already `position: fixed` and appended to the body, so without the top layer it
   * still renders in the right place. It only loses its immunity to a clipping ancestor, which is
   * a visual degradation rather than a failure, and calling the method blindly would instead throw.
   *
   * @param bubble bubble to toggle
   * @param visible whether the bubble must be shown
   */
  private togglePopover(bubble: HTMLElement, visible: boolean): void {
    const toggle = visible ? bubble.showPopover : bubble.hidePopover;
    if (typeof toggle !== 'function') {
      return;
    }

    toggle.call(bubble);
  }

  /**
   * Places the bubble beside the host, keeping it inside the viewport.
   *
   * Measured after the popover is shown, since a hidden popover has no size to center against.
   */
  private position(bubble: HTMLElement): void {
    const anchor = this.host.nativeElement.getBoundingClientRect();
    const size = bubble.getBoundingClientRect();
    const view = this.document().defaultView;
    const viewportWidth = view?.innerWidth ?? 0;
    const viewportHeight = view?.innerHeight ?? 0;

    let top: number;
    let left: number;

    switch (this.appTooltipPosition()) {
      case 'below':
        top = anchor.bottom + OFFSET;
        left = anchor.left + (anchor.width - size.width) / 2;
        break;
      case 'left':
        top = anchor.top + (anchor.height - size.height) / 2;
        left = anchor.left - size.width - OFFSET;
        break;
      case 'right':
        top = anchor.top + (anchor.height - size.height) / 2;
        left = anchor.right + OFFSET;
        break;
      default:
        top = anchor.top - size.height - OFFSET;
        left = anchor.left + (anchor.width - size.width) / 2;
        break;
    }

    this.renderer.setStyle(bubble, 'top', `${this.clamp(top, viewportHeight - size.height)}px`);
    this.renderer.setStyle(bubble, 'left', `${this.clamp(left, viewportWidth - size.width)}px`);
  }

  /**
   * Keeps a coordinate within the viewport, never returning a negative offset.
   *
   * @param value preferred coordinate
   * @param maximum largest coordinate keeping the bubble fully visible
   * @return clamped coordinate
   */
  private clamp(value: number, maximum: number): number {
    return Math.max(OFFSET, Math.min(value, maximum - OFFSET));
  }

  /**
   * Closes the tooltip on Escape, as WAI-ARIA requires of every tooltip.
   *
   * Bound on the document because the host is not necessarily focused: a tooltip opened by hovering
   * would otherwise never receive the key event.
   */
  private listenForEscape(): void {
    this.escapeListener = this.renderer.listen(
      this.document(),
      'keydown',
      (event: KeyboardEvent) => {
        if (event.key === 'Escape') {
          this.hide();
        }
      },
    );
  }

  /**
   * Returns the document owning the host element.
   *
   * Read from the host rather than injected as a global so the directive keeps working in a
   * server-rendered or test document.
   */
  private document(): Document {
    return this.host.nativeElement.ownerDocument;
  }
}
