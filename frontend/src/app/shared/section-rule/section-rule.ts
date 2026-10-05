import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  viewChild,
} from '@angular/core';

import { fitToLines } from './section-rule.utils';

/**
 * Section heading: title, fading rule and closing diamond.
 */
@Component({
  selector: 'app-section-rule',
  templateUrl: './section-rule.html',
  styleUrl: './section-rule.scss',
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
   * Heading element, narrowed to its widest wrapped line.
   */
  private readonly title = viewChild.required<ElementRef<HTMLHeadingElement>>('title');

  /**
   * Keeps the title hugging its text, so the hairline starts where the words end.
   */
  constructor() {
    afterRenderEffect((onCleanup) => {
      this.heading();
      const title = this.title().nativeElement;
      const observer = new ResizeObserver(() => fitToLines(title));
      observer.observe(title.parentElement ?? title);
      onCleanup(() => observer.disconnect());
    });
  }
}
