import { Component, input } from '@angular/core';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { formatScore, formatWinRate } from '@core/players/player-format.utils';
import { resolveWinRateVisual } from '@core/players/player-stats.utils';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { Tooltip } from '@shared/tooltip/tooltip';
import { MediaThumbnail } from '../../media-thumbnail/media-thumbnail';
import { EntityStatsRow } from './entity-stats.model';

/**
 * Win rate, combat score and volume per map or per agent.
 *
 * One component for both: the two tables differ only in where their pictures come from and what
 * their first column is called, and the caller resolves both before handing rows over.
 *
 * A CSS grid rather than a `<table>`, matching the match history right above it — and it collapses
 * to a stacked card list below `sm`, where six columns cannot be read side by side.
 */
@Component({
  selector: 'app-entity-stats',
  imports: [TranslatePipe, MediaThumbnail, ProgressBar, Tooltip],
  templateUrl: './entity-stats.html',
})
export class EntityStats {
  /**
   * Already-translated name of the section.
   */
  public readonly title = input.required<string>();

  /**
   * Already-translated explanation of what the table shows and how it is worked out.
   */
  public readonly titleTooltip = input.required<string>();

  /**
   * Already-translated name of the first column.
   */
  public readonly entityLabel = input.required<string>();

  /**
   * Rows to render, most-played first.
   */
  public readonly rows = input.required<readonly EntityStatsRow[]>();

  /**
   * Formats a win rate, exposed to the template.
   */
  protected readonly formatWinRate = formatWinRate;

  /**
   * Formats an average combat score, exposed to the template.
   */
  protected readonly formatScore = formatScore;

  /**
   * Resolves the colors carrying a win rate, exposed to the template.
   */
  protected readonly winRateVisual = resolveWinRateVisual;
}
