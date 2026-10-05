import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { LowerCasePipe } from '@angular/common';
import { LucideDynamicIcon } from '@lucide/angular';

import { formatFigure } from '@core/i18n/format/number-format.utils';
import { CAMPAIGN_WEEK_COUNT } from '@core/campaign/campaign.constants';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { LineChart } from '@shared/chart/line-chart/line-chart';
import { HistoryCurve, HistoryRow } from '../campaign-panel.model';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import { ICON_COLUMNS } from './campaign-history.constants';

/**
 * Population curves of every campaign and their ranking by final base.
 */
@Component({
  selector: 'app-campaign-history',
  imports: [LowerCasePipe, LucideDynamicIcon, TranslatePipe, LineChart],
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
   * Table columns headed by a concept icon.
   */
  protected readonly iconColumns = ICON_COLUMNS;

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
    return formatFigure(value, this.translation.language());
  }
}
