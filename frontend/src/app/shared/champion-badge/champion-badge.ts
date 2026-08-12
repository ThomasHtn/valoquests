import { Component } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';

/**
 * Title tag marking the player who topped the most recently finalized week's ranking.
 *
 * Shown beside the player's name everywhere it appears across the app, paired with
 * `app-avatar`'s own `champion` input drawing a matching gold ring around their portrait.
 */
@Component({
  selector: 'app-champion-badge',
  templateUrl: './champion-badge.html',
  imports: [TranslatePipe],
  host: { class: 'contents' },
})
export class ChampionBadge {}
