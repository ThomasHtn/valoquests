import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { LucideHeartPulse, LucideSkull, LucideUsers } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { CAMPAIGN_WEEK_COUNT } from '@core/campaign/campaign.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { LineChart } from '@shared/chart/line-chart';
import { HistoryCurve, HistoryRow } from '../campaign.model';

/**
 * The base week after week, the current campaign against the closed ones, and the ranking of
 * every campaign by its final base.
 */
@Component({
  selector: 'app-campaign-history',
  imports: [TranslatePipe, LineChart, LucideHeartPulse, LucideSkull, LucideUsers],
  templateUrl: './campaign-history.html',
  styleUrl: './campaign-history.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CampaignHistoryView {
  public readonly curves = input.required<readonly HistoryCurve[]>();
  public readonly rows = input.required<readonly HistoryRow[]>();

  private readonly translation = inject(Translation);

  protected readonly weekCount = CAMPAIGN_WEEK_COUNT;

  protected readonly series = computed(() => this.curves().map((curve) => curve.series));

  protected readonly xLabels = Array.from({ length: CAMPAIGN_WEEK_COUNT }, (_, index) =>
    String(index + 1).padStart(2, '0'),
  );

  protected format(value: number): string {
    return formatDamage(value, this.translation.language());
  }
}
