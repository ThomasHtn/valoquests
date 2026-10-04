import { Component, inject, input } from '@angular/core';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { formatScore, formatWinRate } from '@core/players/player-format.utils';
import { resolveWinRateVisual } from '@core/players/stats/player-stats.utils';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { Tooltip } from '@shared/tooltip/tooltip';
import { MediaThumbnail } from '@shared/media-thumbnail/media-thumbnail';
import { EntityStatsRow } from './entity-stats.model';

/**
 * Win rate, combat score and volume per map or agent; stacks into cards below `sm`.
 */
@Component({
  selector: 'app-entity-stats',
  imports: [TranslatePipe, MediaThumbnail, ProgressBar, Tooltip],
  templateUrl: './entity-stats.html',
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
   * Formats an average combat score.
   */
  protected readonly formatScore = formatScore;

  /**
   * Win rate colours.
   */
  protected readonly winRateVisual = resolveWinRateVisual;

  /**
   * Formats a win rate.
   */
  protected readonly formatWinRate = (winRate: number | null): string =>
    formatWinRate(winRate, this.translation.language());
}
