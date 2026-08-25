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
 * Delay a tooltip hung off a whole block waits before opening, in milliseconds.
 *
 * Long enough that crossing the block on the way somewhere else never opens the bubble, short
 * enough that stopping on it to ask "what is this" does not feel like waiting. Shared so the two
 * overview blocks answer at the same pace rather than each picking a number.
 */
export const BLOCK_TOOLTIP_DELAY_MS = 400;

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
    '(mouseenter)': 'scheduleShow()',
    '(mouseleave)': 'hide()',
    // Focus opens the bubble at once: the delay exists to keep a pointer crossing the host from
    // flashing it, and a keyboard user does not cross anything.
    '(focusin)': 'showOnHostFocus($event)',
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
   * How long the pointer must rest on the host before the bubble opens, in milliseconds.
   *
   * Zero by default, which is what a tooltip hanging off a small target wants: the reader aimed at
   * that word or that figure, so the answer is owed immediately. A host covering a whole block is
   * the opposite case — the pointer crosses it on the way to anything else, and without a delay the
   * bubble flashes over content the reader was heading for.
   */
  public readonly appTooltipDelay = input(0);

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
   * Pending {@link appTooltipDelay} timer, or `null` while none is armed.
   */
  private showTimer: ReturnType<typeof setTimeout> | null = null;

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
   * Opens the bubble when the host itself takes focus, ignoring focus landing on a descendant.
   *
   * `focusin` bubbles, unlike `mouseenter`, so a host wrapping its own interactive content — the
   * podium, whose every row is a link to a player — would otherwise open its bubble on each of
   * them in turn while the reader tabs through. The bubble describes the host, so only the host
   * taking focus is a reason to show it.
   *
   * @param event - The focus event that reached the host.
   */
  protected showOnHostFocus(event: FocusEvent): void {
    if (event.target !== this.host.nativeElement) {
      return;
    }

    this.show();
  }

  /**
   * Opens the bubble once the pointer has rested on the host for {@link appTooltipDelay}.
   *
   * Falls through to {@link show} with no timer at all when no delay is configured, so the default
   * call site keeps its immediate bubble and gains no scheduling.
   */
  protected scheduleShow(): void {
    const delay = this.appTooltipDelay();
    if (delay <= 0) {
      this.show();

      return;
    }

    this.cancelScheduledShow();
    this.showTimer = setTimeout(() => {
      this.showTimer = null;
      this.show();
    }, delay);
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
   *
   * Disarms any pending delay first, and before the early return: a pointer that leaves the host
   * before the timer fires must not have a bubble open behind it.
   */
  protected hide(): void {
    this.cancelScheduledShow();

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
   * Disarms the pending delay timer, if any.
   */
  private cancelScheduledShow(): void {
    if (this.showTimer === null) {
      return;
    }

    clearTimeout(this.showTimer);
    this.showTimer = null;
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
