import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideHeartPulse, LucideUsers, LucideWheat, LucideZap } from '@lucide/angular';

import { ColonyApi } from '@core/colony/colony-api';
import { formatGauge, formatPopulation } from '@core/colony/colony-format.utils';
import { colonyGaugeColors, colonyHealthColors } from '@core/colony/colony-gauge.utils';
import { Colony, ColonyGaugeState } from '@core/colony/colony.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resourceValue } from '@core/http/resource-state.utils';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { BLOCK_TOOLTIP_DELAY_MS, Tooltip } from '@shared/tooltip/tooltip';

/**
 * The three tracks the block draws, in the order they are read: the two gauges that are fed, then
 * the health they produce.
 */
type ColonyTrack = 'FOOD' | 'ENERGY' | 'HEALTH';

/**
 * One track of the summary, resolved into everything the template positions.
 */
interface ColonyTrackRow {
  readonly track: ColonyTrack;

  /**
   * Already-translated name, read out beside the bar for assistive technology — the row itself
   * only carries a glyph.
   */
  readonly label: string;

  readonly percentage: number;

  /**
   * Already-formatted value: one decimal on a gauge, a whole point on health.
   */
  readonly valueLabel: string;

  readonly fillClass: string;
  readonly textClass: string;

  /**
   * Level the bar draws its reference tick at, `null` on health, which settles at nothing of its
   * own — it is the mean the two gauges above it produce.
   */
  readonly equilibriumPercentage: number | null;
}

/**
 * How far the seat inside a hexagon is scaled down from the outline around it, so the outline
 * reads as a rim of even thickness on every side. Two figures because the rim has to keep its
 * apparent thickness on the population hexagon and on the much smaller gauge badges alike.
 *
 * Restated here rather than imported from the campaign page's own map constants: a page must not
 * reach into another page's internals, and these two figures are the direction's hexagon, not the
 * map's.
 */
const HEXAGON_INNER_SCALE = 0.95;
const GLYPH_INNER_SCALE = 0.84;

/**
 * Compact readout of the run in progress for the overview: the population it stands at, the
 * resources holding it up, and how far into the run today is.
 *
 * The same three figures the campaign page opens on, in the order it states them, so the block is
 * recognised again on the page it links to. It stops there: the buildings, the fights and the
 * curve answer "how is the run going" in full and have a page of their own — this one only says
 * whether that page is worth opening today.
 *
 * Reads the colony resource directly rather than through `ColonyView`, which also pulls the curve,
 * the history and the run's fights: none of them is drawn here, and the overview is the landing
 * screen, where four requests nobody reads are four requests too many.
 *
 * Renders nothing at all until the colony resolves, rather than a placeholder: it is a complement
 * to the week's own blocks, not one of them, so a failed request must not put an error state in the
 * middle of a page that is otherwise fine.
 */
@Component({
  selector: 'app-colony-summary',
  imports: [
    TranslatePipe,
    RouterLink,
    ProgressBar,
    Tooltip,
    LucideHeartPulse,
    LucideUsers,
    LucideWheat,
    LucideZap,
  ],
  templateUrl: './colony-summary.html',
  // Transparent host: the section itself becomes the flex item of the overview's left column, so
  // it sits at its natural height and leaves the rest of the column to the progress panel below.
  host: { class: 'contents' },
})
export class ColonySummary {
  /**
   * Data-access service backing the colony, shared with the campaign page itself.
   */
  private readonly colonyApi = inject(ColonyApi);

  /**
   * i18n service resolving the summary's labels.
   */
  private readonly translation = inject(Translation);

  /**
   * Scales the template applies to the seat inside a hexagon.
   */
  protected readonly hexagonInnerScale = HEXAGON_INNER_SCALE;
  protected readonly glyphInnerScale = GLYPH_INNER_SCALE;

  /**
   * How long the pointer rests on the block before its tooltip opens.
   */
  protected readonly tooltipDelayMs = BLOCK_TOOLTIP_DELAY_MS;

  /**
   * The colony, or `null` while it has not resolved.
   */
  protected readonly colony = computed<Colony | null>(
    () => resourceValue(this.colonyApi.colony, null) ?? null,
  );

  /**
   * Already-formatted population.
   *
   * The figure alone, with no capacity beside it: what the hexagon's fill height already says is
   * not restated in text, and the campaign page carries the full reading.
   */
  protected readonly populationLabel = computed<string>(() => {
    const colony = this.colony();

    return colony === null ? '' : formatPopulation(colony.population, this.translation.language());
  });

  /**
   * The two gauges and the health they produce, on one track each — the campaign page's resource
   * band, reduced to the three bars.
   */
  protected readonly tracks = computed<readonly ColonyTrackRow[]>(() => {
    const colony = this.colony();
    if (colony === null) {
      return [];
    }

    const health = colonyHealthColors(colony.alert);

    return [
      this.toTrackRow('FOOD', colony.food, colony.alert),
      this.toTrackRow('ENERGY', colony.energy, colony.alert),
      {
        track: 'HEALTH',
        label: this.translation.translate('colony.health'),
        percentage: colony.healthPercentage,
        // Whole points: fractions would suggest a precision the geometric mean of two moving
        // gauges does not have. Same rounding as the campaign page's own health figure.
        valueLabel: `${Math.round(colony.healthPercentage)}`,
        fillClass: health.fill,
        textClass: health.text,
        equilibriumPercentage: null,
      },
    ];
  });

  /**
   * How far into the run today is, `Jour 3 / 71`.
   */
  protected readonly runDayLabel = computed<string>(() => {
    const colony = this.colony();

    return colony === null
      ? ''
      : this.translation.translate('colony.runDay', {
          day: colony.runDay,
          days: colony.runDayCount,
        });
  });

  /**
   * Resolves one gauge into its track row.
   *
   * @param track - Which gauge this is.
   * @param state - Its value and the level it settles at.
   * @param alert - Whether the colony has fallen under the distress threshold.
   * @returns The display-ready track.
   */
  private toTrackRow(
    track: 'FOOD' | 'ENERGY',
    state: ColonyGaugeState,
    alert: boolean,
  ): ColonyTrackRow {
    const colors = colonyGaugeColors(track, alert);

    return {
      track,
      label: this.translation.translate(`colony.gauge.${track}.name`),
      percentage: state.value,
      valueLabel: formatGauge(state.value, this.translation.language()),
      fillClass: colors.fill,
      textClass: colors.text,
      equilibriumPercentage: state.equilibrium,
    };
  }
}
