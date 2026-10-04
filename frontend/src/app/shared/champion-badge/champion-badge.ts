import { Component, input } from '@angular/core';

import { TitleBadge } from '@shared/title-badge/title-badge';

/**
 * Title of the last finalized week's winner, paired with the avatar's `champion` ring.
 */
@Component({
  selector: 'app-champion-badge',
  templateUrl: './champion-badge.html',
  imports: [TitleBadge],
  host: { class: 'contents' },
})
export class ChampionBadge {
  /**
   * `sm` in dense rows, `md` next to a heading.
   */
  public readonly size = input<'sm' | 'md'>('sm');

  /**
   * Inside a link, forwarded to the title badge.
   */
  public readonly inLink = input(false);
}
