import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { LucideDynamicIcon } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { CAMPAIGN_WEEK_COUNT } from '@core/campaign/campaign.constants';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { LineChart } from '@shared/chart/line-chart/line-chart';
import { HistoryCurve, HistoryRow } from '../campaign-panel.model';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';

/**
 * Population curves of every campaign and their ranking by final base.
 */
@Component({
  selector: 'app-campaign-history',
  imports: [LucideDynamicIcon, TranslatePipe, LineChart],
  templateUrl: './campaign-history.html',
  styleUrl: './campaign-history.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CampaignHistoryView {
  /**
   * Concept icons for the template.
   */
  protected readonly concepts = CONCEPT_ICONS;

  /**
   * Population curve of each campaign, with its legend figure.
   */
  public readonly curves = input.required<readonly HistoryCurve[]>();

  /**
   * Campaigns ranked by final base, one table row each.
   */
  public readonly rows = input.required<readonly HistoryRow[]>();

  /**
   * Active language, to format figures with its separators.
   */
  private readonly translation = inject(Translation);

  /**
   * Campaign length, the guardian total of a finished campaign.
   */
  protected readonly weekCount = CAMPAIGN_WEEK_COUNT;

  /**
   * Chart series extracted from the curves.
   */
  protected readonly series = computed(() => this.curves().map((curve) => curve.series));

  /**
   * Two-digit week numbers along the chart axis.
   */
  protected readonly xLabels = Array.from({ length: CAMPAIGN_WEEK_COUNT }, (_, index) =>
    String(index + 1).padStart(2, '0'),
  );

  /**
   * Formats a figure in the active language for the legend and table.
   */
  protected format(value: number): string {
    return formatDamage(value, this.translation.language());
  }
}
