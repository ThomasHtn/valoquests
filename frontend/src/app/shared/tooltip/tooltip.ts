import {
  ApplicationRef,
  Directive,
  ElementRef,
  EmbeddedViewRef,
  Injector,
  OnDestroy,
  Renderer2,
  TemplateRef,
  inject,
  input,
  signal,
} from '@angular/core';

import { nextInstanceId } from '@core/dom/instance-id.utils';
import { TOOLTIP_FALLBACK_ICON, TOOLTIP_OFFSET } from './tooltip.constants';
import { TooltipLayout, TooltipPosition, TooltipTrigger } from './tooltip.model';

/**
 * Text bubble shown on hover and focus (or tap for an info button), closed on Escape.
 * A native popover in the top layer, so no `overflow: hidden` ancestor can clip it.
 */
@Directive({
  selector: '[appTooltip]',
  host: {
    '(mouseenter)': 'onMouseEnter()',
    '(mouseleave)': 'onMouseLeave()',
    '(pointerenter)': 'onPointerEnter($event)',
    '(pointerleave)': 'onPointerLeave($event)',
    '(click)': 'toggle($event)',
    // Focus skips the delay, which only guards against a pointer crossing the host.
    '(focusin)': 'showOnHostFocus($event)',
    '(focusout)': 'hide()',
  },
})
export class Tooltip implements OnDestroy {
  /**
   * Translated text of the bubble.
   */
  public readonly appTooltip = input.required<string>();

  /**
   * Side of the host the bubble sits on.
   */
  public readonly appTooltipPosition = input<TooltipPosition>('above');

  /**
   * Suppresses the tooltip, e.g. sidebar entries while the rail is expanded.
   */
  public readonly appTooltipDisabled = input(false);

  /**
   * Bubble size: `md` for the collapsed sidebar's labels, `sm` elsewhere.
   */
  public readonly appTooltipSize = input<'sm' | 'md'>('sm');

  /**
   * `click` for an info button: mouse hover opens it, tap or keyboard toggles it.
   */
  public readonly appTooltipTrigger = input<TooltipTrigger>('hover');

  /**
   * Player portrait before the text: URL, `null` for the fallback disc, `undefined` for none.
   */
  public readonly appTooltipPortrait = input<string | null | undefined>(undefined);

  /**
   * Rich content shown instead of the text; the text stays required for assistive tech.
   */
  public readonly appTooltipTemplate = input<TemplateRef<unknown> | null>(null);

  /**
   * Host the bubble is placed against and describes.
   */
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /**
   * Builds the bubble outside any template.
   */
  private readonly renderer = inject(Renderer2);

  /**
   * Keeps the template's view under change detection while shown.
   */
  private readonly appRef = inject(ApplicationRef);

  /**
   * Injector of the template's view.
   */
  private readonly injector = inject(Injector);

  /**
   * View of {@link appTooltipTemplate}, `null` while hidden or text-only.
   */
  private contentView: EmbeddedViewRef<unknown> | null = null;

  /**
   * Bubble id referenced by the host's `aria-describedby`.
   */
  private readonly tooltipId = nextInstanceId('app-tooltip');

  /**
   * Bubble in the document, `null` while hidden.
   */
  private readonly bubble = signal<HTMLElement | null>(null);

  /**
   * Removes the Escape listener, `null` while hidden.
   */
  private escapeListener: (() => void) | null = null;

  /**
   * Removes the outside-click listener, `null` while hidden.
   */
  private outsideListener: (() => void) | null = null;

  /**
   * Removes the scroll and resize listeners, `null` while hidden.
   */
  private movementListener: (() => void) | null = null;

  /**
   * The popover lives on the body, so it would otherwise outlive its host.
   */
  public ngOnDestroy(): void {
    this.hide();
  }

  /**
   * Opens on focus of the host itself: `focusin` also bubbles up from focusable descendants.
   */
  protected showOnHostFocus(event: FocusEvent): void {
    if (this.isClickTriggered() || event.target !== this.host.nativeElement) {
      return;
    }

    this.show();
  }

  /**
   * Opens a hover tooltip when the mouse enters the host.
   */
  protected onMouseEnter(): void {
    if (!this.isClickTriggered()) {
      this.show();
    }
  }

  /**
   * Closes a hover tooltip when the mouse leaves the host.
   */
  protected onMouseLeave(): void {
    if (!this.isClickTriggered()) {
      this.hide();
    }
  }

  /**
   * Opens a click tooltip under a real mouse only; touch must go on tapping.
   */
  protected onPointerEnter(event: PointerEvent): void {
    if (this.isClickTriggered() && event.pointerType === 'mouse') {
      this.show();
    }
  }

  /**
   * Closes a click tooltip that mouse hover opened, once the mouse leaves.
   */
  protected onPointerLeave(event: PointerEvent): void {
    if (this.isClickTriggered() && event.pointerType === 'mouse') {
      this.hide();
    }
  }

  /**
   * Toggles a click tooltip on tap or keyboard; a mouse click must not close what hover opened.
   */
  protected toggle(event: MouseEvent): void {
    if (!this.isClickTriggered()) {
      return;
    }

    if (event instanceof PointerEvent && event.pointerType === 'mouse') {
      return;
    }

    if (this.bubble()) {
      this.hide();
    } else {
      this.show();
    }
  }

  /**
   * Whether the tooltip is an info button's, opened by tap rather than hover.
   */
  private isClickTriggered(): boolean {
    return this.appTooltipTrigger() === 'click';
  }

  /**
   * Builds and places the bubble; a no-op when disabled, blank or already shown.
   */
  protected show(): void {
    if (this.appTooltipDisabled() || !this.appTooltip().trim() || this.bubble()) {
      return;
    }

    const bubble = this.renderer.createElement('div') as HTMLElement;
    this.renderer.setAttribute(bubble, 'id', this.tooltipId);
    this.renderer.setAttribute(bubble, 'role', 'tooltip');
    this.renderer.setAttribute(bubble, 'popover', 'manual');
    const portrait = this.appTooltipPortrait();
    const template = this.appTooltipTemplate();
    let layout: TooltipLayout = this.appTooltipSize();
    if (template) {
      this.fillWithTemplate(bubble, template);
      layout = 'template';
    } else if (portrait === undefined) {
      this.renderer.setProperty(bubble, 'textContent', this.appTooltip());
    } else {
      this.fillWithPortrait(bubble, portrait);
      layout = 'portrait';
    }
    this.renderer.setAttribute(bubble, 'class', `tooltip tooltip--${layout} notch-tr fx-tip-in`);
    this.renderer.setStyle(bubble, 'position', 'fixed');

    this.renderer.appendChild(this.document().body, bubble);
    this.togglePopover(bubble, true);

    this.position(bubble);
    this.renderer.setAttribute(this.host.nativeElement, 'aria-describedby', this.tooltipId);
    this.bubble.set(bubble);
    this.listenForEscape();
    this.listenForMovement();
    if (this.isClickTriggered()) {
      this.listenForOutsideClick();
    }
  }

  /**
   * Renders the template, checked once so the bubble is sized before placement.
   */
  private fillWithTemplate(bubble: HTMLElement, template: TemplateRef<unknown>): void {
    const view = template.createEmbeddedView({}, this.injector);
    this.appRef.attachView(view);
    view.detectChanges();
    for (const node of view.rootNodes) {
      this.renderer.appendChild(bubble, node);
    }
    this.contentView = view;
  }

  /**
   * Fills the bubble with a portrait disc (fallback when `null`) then the text.
   */
  private fillWithPortrait(bubble: HTMLElement, portrait: string | null): void {
    const frame = portrait ? this.portraitImage(portrait) : this.portraitFallback();
    const text = this.renderer.createElement('span') as HTMLElement;
    this.renderer.setProperty(text, 'textContent', this.appTooltip());
    this.renderer.appendChild(bubble, frame);
    this.renderer.appendChild(bubble, text);
  }

  /**
   * Decorative portrait image: the player's name follows as text.
   */
  private portraitImage(portrait: string): HTMLElement {
    const image = this.renderer.createElement('img') as HTMLElement;
    this.renderer.setAttribute(image, 'src', portrait);
    this.renderer.setAttribute(image, 'alt', '');
    this.renderer.setAttribute(image, 'width', '36');
    this.renderer.setAttribute(image, 'height', '36');
    this.renderer.setAttribute(image, 'class', 'tooltip__portrait');
    return image;
  }

  /**
   * Fallback disc with Lucide's `user` glyph.
   */
  private portraitFallback(): HTMLElement {
    const svgNamespace = 'svg';
    const disc = this.renderer.createElement('span') as HTMLElement;
    this.renderer.setAttribute(disc, 'class', 'tooltip__portrait tooltip__portrait--fallback');
    const icon = this.renderer.createElement('svg', svgNamespace) as SVGElement;
    const iconAttributes: Readonly<Record<string, string>> = {
      class: 'tooltip__portrait-icon',
      viewBox: '0 0 24 24',
      fill: 'none',
      stroke: 'currentColor',
      'stroke-width': '2',
      'stroke-linecap': 'round',
      'stroke-linejoin': 'round',
    };
    for (const [name, value] of Object.entries(iconAttributes)) {
      this.renderer.setAttribute(icon, name, value);
    }
    for (const node of TOOLTIP_FALLBACK_ICON) {
      const shape = this.renderer.createElement(node.tag, svgNamespace) as SVGElement;
      for (const [name, value] of Object.entries(node.attributes)) {
        this.renderer.setAttribute(shape, name, value);
      }
      this.renderer.appendChild(icon, shape);
    }
    this.renderer.appendChild(disc, icon);
    return disc;
  }

  /**
   * Removes the bubble and its listeners.
   */
  protected hide(): void {
    const bubble = this.bubble();
    if (!bubble) {
      return;
    }

    this.escapeListener?.();
    this.escapeListener = null;
    this.outsideListener?.();
    this.outsideListener = null;
    this.movementListener?.();
    this.movementListener = null;

    this.contentView?.destroy();
    this.contentView = null;

    this.renderer.removeAttribute(this.host.nativeElement, 'aria-describedby');
    this.togglePopover(bubble, false);
    this.renderer.removeChild(this.document().body, bubble);
    this.bubble.set(null);
  }

  /**
   * Toggles the popover; skipped where unsupported, the fixed bubble still renders.
   */
  private togglePopover(bubble: HTMLElement, visible: boolean): void {
    const toggle = visible ? bubble.showPopover : bubble.hidePopover;
    if (typeof toggle !== 'function') {
      return;
    }

    toggle.call(bubble);
  }

  /**
   * Places the bubble beside the host in the viewport; call once shown so it has a size.
   */
  private position(bubble: HTMLElement): void {
    const anchor = this.host.nativeElement.getBoundingClientRect();
    // Layout size: the entrance animation scales the painted box on its first frame.
    const size = { width: bubble.offsetWidth, height: bubble.offsetHeight };
    const view = this.document().defaultView;
    const viewportWidth = view?.innerWidth ?? 0;
    const viewportHeight = view?.innerHeight ?? 0;

    let top: number;
    let left: number;

    switch (this.appTooltipPosition()) {
      case 'below':
        top = anchor.bottom + TOOLTIP_OFFSET;
        left = anchor.left + (anchor.width - size.width) / 2;
        break;
      case 'left':
        top = anchor.top + (anchor.height - size.height) / 2;
        left = anchor.left - size.width - TOOLTIP_OFFSET;
        break;
      case 'right':
        top = anchor.top + (anchor.height - size.height) / 2;
        left = anchor.right + TOOLTIP_OFFSET;
        break;
      default:
        top = anchor.top - size.height - TOOLTIP_OFFSET;
        left = anchor.left + (anchor.width - size.width) / 2;
        break;
    }

    this.renderer.setStyle(bubble, 'top', `${this.clamp(top, viewportHeight - size.height)}px`);
    this.renderer.setStyle(bubble, 'left', `${this.clamp(left, viewportWidth - size.width)}px`);
  }

  /**
   * Clamps a coordinate so the bubble keeps {@link TOOLTIP_OFFSET} from the viewport edges.
   */
  private clamp(value: number, maximum: number): number {
    return Math.max(TOOLTIP_OFFSET, Math.min(value, maximum - TOOLTIP_OFFSET));
  }

  /**
   * Closes on Escape, captured on the window since a hovered host has no focus.
   * The key is consumed so it does not also close the dialog or drawer underneath.
   */
  private listenForEscape(): void {
    const view = this.document().defaultView;
    if (!view) {
      return;
    }
    const onKeydown = (event: KeyboardEvent): void => {
      if (event.key === 'Escape') {
        event.preventDefault();
        event.stopImmediatePropagation();
        this.hide();
      }
    };
    view.addEventListener('keydown', onKeydown, { capture: true });
    this.escapeListener = () => view.removeEventListener('keydown', onKeydown, { capture: true });
  }

  /**
   * Follows the host on scroll and resize, closing once it is off screen (touch never leaves).
   */
  private listenForMovement(): void {
    const view = this.document().defaultView;
    if (!view) {
      return;
    }
    const follow = (): void => {
      const bubble = this.bubble();
      const rect = this.host.nativeElement.getBoundingClientRect();
      const offScreen = rect.bottom < 0 || rect.top > view.innerHeight;
      if (!bubble || offScreen) {
        this.hide();
        return;
      }
      this.position(bubble);
    };
    view.addEventListener('scroll', follow, { capture: true, passive: true });
    view.addEventListener('resize', follow, { passive: true });
    this.movementListener = () => {
      view.removeEventListener('scroll', follow, { capture: true });
      view.removeEventListener('resize', follow);
    };
  }

  /**
   * Closes a click tooltip on a pointer down outside its host.
   */
  private listenForOutsideClick(): void {
    this.outsideListener = this.renderer.listen(
      this.document(),
      'pointerdown',
      (event: PointerEvent) => {
        if (!this.host.nativeElement.contains(event.target as Node)) {
          this.hide();
        }
      },
    );
  }

  /**
   * Host's own document, so the directive works in a server or test document.
   */
  private document(): Document {
    return this.host.nativeElement.ownerDocument;
  }
}
