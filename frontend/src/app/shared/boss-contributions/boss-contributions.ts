import { Component, input } from '@angular/core';

import { BossContribution } from '@core/boss/boss-timeline.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { PositionBadge } from '@shared/position-badge/position-badge';

/**
 * A week's damage broken down per player, ranked.
 *
 * Shared between the boss detail drawer and the campaign page's active-boss panel, which both read
 * off the same `BossTimelineNode.contributions` and used to duplicate the same row markup.
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
}
