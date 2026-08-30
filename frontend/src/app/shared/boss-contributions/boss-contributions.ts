import { Component, computed, input } from '@angular/core';

import { BossContribution } from '@core/boss/boss-timeline.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { PositionBadge } from '@shared/position-badge/position-badge';

/**
 * A week's damage broken down per player, ranked.
 *
 * Renders the whole board by default, or its head alone when {@link limit} is set — the campaign's
 * week panel only has room for a podium beside the health bar and the rewards, and links out to the
 * full ranking rather than repeating it.
 */
@Component({
  selector: 'app-boss-contributions',
  imports: [TranslatePipe, Avatar, ChampionBadge, PositionBadge],
  templateUrl: './boss-contributions.html',
})
export class BossContributions {
  /**
   * The week's damage broken down per player, best first. Renders nothing when empty.
   */
  public readonly contributions = input.required<readonly BossContribution[]>();

  /**
   * How many rows to keep, best first, or `null` for the whole board.
   */
  public readonly limit = input<number | null>(null);

  /**
   * Translation key naming the list, so a truncated board can say what it is showing rather than
   * claiming to be the ranking itself.
   */
  public readonly headingKey = input('boss.panel.ranking');

  /**
   * The rows actually rendered: the whole board, or its first {@link limit} entries.
   */
  protected readonly rows = computed<readonly BossContribution[]>(() => {
    const limit = this.limit();
    const contributions = this.contributions();

    return limit === null ? contributions : contributions.slice(0, limit);
  });
}
