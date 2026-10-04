import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  input,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';

import { fitToLines } from './section-rule.utils';

/**
 * Section heading: title, fading rule, optional link and closing diamond.
 */
@Component({
  selector: 'app-section-rule',
  imports: [RouterLink],
  templateUrl: './section-rule.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SectionRule {
  /**
   * Section title.
   */
  public readonly heading = input.required<string>();

  /**
   * Title `id`, so the section can be labelled by it.
   */
  public readonly headingId = input.required<string>();

  /**
   * Label of the right-hand link.
   */
  public readonly linkLabel = input('');

  /**
   * Route the link leads to.
   */
  public readonly link = input<string | null>(null);

  /**
   * Fragment of the linked page to land on.
   */
  public readonly linkFragment = input<string | undefined>(undefined);

  /**
   * Caption on the right, such as a count.
   */
  public readonly side = input('');

  /**
   * Whether projected content takes its own line on a phone (it sets its own `order`/basis).
   */
  public readonly wrapSide = input(false);

  /**
   * Whether the hairline stays on a phone; a caption needs its room.
   */
  protected readonly keepsLine = computed(() => this.wrapSide() || !this.side());

  /**
   * Whether the right-hand group dissolves on a phone so its parts can wrap apart.
   */
  protected readonly dissolves = computed(() => this.wrapSide() || !!this.link());

  /**
   * Heading element, narrowed to its widest wrapped line.
   */
  private readonly title = viewChild.required<ElementRef<HTMLHeadingElement>>('title');

  constructor() {
    // A wrapped title hugs its text so the hairline starts where the words end.
    afterRenderEffect((onCleanup) => {
      this.heading();
      const title = this.title().nativeElement;
      const observer = new ResizeObserver(() => fitToLines(title));
      observer.observe(title.parentElement ?? title);
      onCleanup(() => observer.disconnect());
    });
  }
}
