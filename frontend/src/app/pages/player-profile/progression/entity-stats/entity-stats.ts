import { Component, computed, inject, input } from '@angular/core';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { formatScore, formatWinRate } from '@core/players/player-format.utils';
import { resolveWinRateVisual } from '@core/players/stats/player-stats.utils';
import { MediaThumbnail } from '@shared/media-thumbnail/media-thumbnail';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { Tooltip } from '@shared/tooltip/tooltip';

import { EntityStatsDisplayRow, EntityStatsRow } from './entity-stats.model';

/**
 * Win rate, combat score and volume per map or agent; stacks into cards below `sm`.
 */
@Component({
  selector: 'app-entity-stats',
  imports: [TranslatePipe, MediaThumbnail, ProgressBar, Tooltip],
  templateUrl: './entity-stats.html',
  styleUrl: './entity-stats.scss',
})
export class EntityStats {
  /**
   * Translated section name.
   */
  public readonly title = input.required<string>();

  /**
   * Translated explanation of the table.
   */
  public readonly titleTooltip = input.required<string>();

  /**
   * Translated name of the first column.
   */
  public readonly entityLabel = input.required<string>();

  /**
   * Rows, most played first.
   */
  public readonly rows = input.required<readonly EntityStatsRow[]>();

  /**
   * Translation service, whose language picks the win rate notation.
   */
  private readonly translation = inject(Translation);

  /**
   * Rows with their figures formatted; a row without a match keeps a neutral win rate.
   */
  protected readonly displayRows = computed<readonly EntityStatsDisplayRow[]>(() => {
    const language = this.translation.language();
    return this.rows().map((row) => ({
      ...row,
      winRateLabel: formatWinRate(row.winRate, language),
      winRateVisual: resolveWinRateVisual(row.matchesPlayed > 0 ? row.winRate : null),
      acsLabel: formatScore(row.acs),
    }));
  });
}
