import { Component, input } from '@angular/core';

/**
 * The opening beat every routed page shares: a brand-colored eyebrow naming the context (the
 * active week, the section of the backoffice), the page's title under it in the display face, and
 * whatever that page pins to the trailing edge of the row — a countdown, a way back.
 *
 * Extracted because the same three elements were written out on nine screens and had already
 * drifted apart: the public pages set the eyebrow at `text-2xs`, the backoffice ones a step up at
 * `text-xs`, for no reason either could state. The size kept here is `text-2xs`, which is what the
 * `--text-2xs` token was introduced for (see `typography.css`).
 *
 * Two projection slots, since a header is otherwise only ever these two shapes:
 * - `[headingAside]` sits on the title's own line, for a chip qualifying it (the rulebook's
 *   reading time).
 * - the default slot sits at the row's trailing edge, for the block a page opens with beside its
 *   title (the countdown to the weekly rollover).
 */
@Component({
  selector: 'app-page-header',
  templateUrl: './page-header.html',
  host: { class: 'block' },
})
export class PageHeader {
  /**
   * Already-translated context line above the title.
   */
  public readonly eyebrow = input.required<string>();

  /**
   * Already-translated page title, rendered as the page's `<h1>`.
   */
  public readonly heading = input.required<string>();
}
