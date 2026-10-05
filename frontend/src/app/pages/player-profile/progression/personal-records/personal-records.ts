import { Component, computed, inject, input } from '@angular/core';
import { LucideDynamicIcon } from '@lucide/angular';

import { formatFigure } from '@core/i18n/format/number-format.utils';
import { formatCampaignDayMonth } from '@core/date/date-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import {
  resolveCompetitiveTierIconUrl,
  resolveCompetitiveTierVisual,
} from '@core/players/competitive-tier/player-competitive-tier.utils';
import {
  formatHeadshotPercentage,
  formatKda,
  formatScore,
} from '@core/players/player-format.utils';
import {
  PersonalRecords as PersonalRecordsData,
  RecordEntry,
} from '@core/players/progression/player-progression.model';
import { RankIconView } from '@shared/rank-icon-view/rank-icon-view';
import { Tooltip } from '@shared/tooltip/tooltip';
import { RECORD_ICONS } from './personal-records.constants';
import { RecordKey, RecordTile } from './personal-records.model';

/**
 * Personal bests as tiles; highs only, and a record never set is left out rather than zero.
 */
@Component({
  selector: 'app-personal-records',
  imports: [LucideDynamicIcon, RankIconView, TranslatePipe, Tooltip],
  templateUrl: './personal-records.html',
  styleUrl: './personal-records.scss',
})
export class PersonalRecords {
  /**
   * Records from the API.
   */
  public readonly records = input.required<PersonalRecordsData>();

  /**
   * Translates each record's context tooltip.
   */
  private readonly translation = inject(Translation);

  /**
   * Icon of each record.
   */
  protected readonly icons = RECORD_ICONS;

  /**
   * Records worth showing, in display order.
   */
  protected readonly tiles = computed<readonly RecordTile[]>(() => {
    const records = this.records();
    const tiles: RecordTile[] = [];

    // Grouped like everywhere else: a best game runs into five figures.
    const language = this.translation.language();
    const groupedScore = (value: number): string =>
      Number.isFinite(value) ? formatFigure(Math.round(value), language) : '—';

    this.pushMatchRecord(tiles, 'mostKills', records.mostKills, (value) => String(value));
    this.pushMatchRecord(tiles, 'bestAcs', records.bestAcs, formatScore);
    this.pushMatchRecord(tiles, 'mostDamage', records.mostDamage, groupedScore);
    this.pushMatchRecord(tiles, 'bestKda', records.bestKda, (value) => formatKda(value, language));
    this.pushMatchRecord(tiles, 'bestHeadshotPercentage', records.bestHeadshotPercentage, (value) =>
      formatHeadshotPercentage(value, language),
    );

    if (records.longestWinStreak > 0) {
      tiles.push(this.simpleTile('longestWinStreak', String(records.longestWinStreak)));
    }
    if (records.longestActiveDayStreak > 0) {
      tiles.push(this.simpleTile('longestActiveDayStreak', String(records.longestActiveDayStreak)));
    }
    if (records.mvps > 0) {
      tiles.push(this.simpleTile('mvps', String(records.mvps)));
    }
    if (records.peakTier) {
      const tier = resolveCompetitiveTierVisual(records.peakTier, (key) =>
        this.translation.translate(key),
      );
      tiles.push({
        ...this.simpleTile('peakTier', tier.label),
        rankIcon: { src: resolveCompetitiveTierIconUrl(records.peakTier), label: tier.label },
      });
    }

    return tiles;
  });

  /**
   * Appends a per-match record, unless no match ever set it.
   */
  private pushMatchRecord(
    tiles: RecordTile[],
    key: RecordKey,
    entry: RecordEntry | null,
    format: (value: number) => string,
  ): void {
    if (!entry) {
      return;
    }

    tiles.push({
      key,
      value: format(entry.value),
      tooltip: this.translation.translate(`playerProfile.progression.records.tooltip.${key}`, {
        map: entry.mapName,
        agent: entry.agentName,
        date: formatCampaignDayMonth(entry.achievedAt, this.translation.language()),
      }),
      rankIcon: null,
    });
  }

  /**
   * Record with no single match behind it.
   */
  private simpleTile(key: RecordKey, value: string): RecordTile {
    return {
      key,
      value,
      tooltip: this.translation.translate(`playerProfile.progression.records.tooltip.${key}`),
      rankIcon: null,
    };
  }
}
