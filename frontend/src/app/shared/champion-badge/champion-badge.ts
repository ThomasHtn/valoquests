import { Component, input } from '@angular/core';

import { TitleBadge } from '@shared/title-badge/title-badge';

/**
 * Title tag marking the player who topped the most recently finalized week's ranking.
 *
 * Shown beside the player's name everywhere it appears across the app, paired with
 * `app-avatar`'s own `champion` input drawing a matching gold ring around their portrait.
 */
@Component({
  selector: 'app-champion-badge',
  templateUrl: './champion-badge.html',
  imports: [TitleBadge],
  host: { class: 'contents' },
})
export class ChampionBadge {
  /**
   * Icon size: `sm` in dense rows, `md` next to a heading.
   */
  public readonly size = input<'sm' | 'md'>('sm');
}
