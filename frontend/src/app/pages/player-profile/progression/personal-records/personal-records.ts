import { Component, computed, inject, input } from '@angular/core';
import {
  LucideCalendarCheck,
  LucideCrosshair,
  LucideShieldCheck,
  LucideStar,
  LucideTrendingUp,
  LucideGauge,
  LucideBomb,
  LucideMedal,
  LucideLocateFixed,
} from '@lucide/angular';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { formatLocalDayMonth } from '@core/date/date-format.utils';
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
import { RecordKey, RecordTile } from './personal-records.model';

/**
 * Personal bests as tiles; highs only, and a record never set is left out rather than zero.
 */
@Component({
  selector: 'app-personal-records',
  imports: [
    LucideLocateFixed,
    LucideMedal,
    LucideBomb,
    LucideGauge,
    TranslatePipe,
    Tooltip,
    RankIconView,
    LucideCrosshair,
    LucideTrendingUp,
    LucideCalendarCheck,
    LucideStar,
    LucideShieldCheck,
  ],
  templateUrl: './personal-records.html',
})
export class PersonalRecords {
  /**
   * Records from the API.
   */
  public readonly records = input.required<PersonalRecordsData>();

  /**
   * Most records shown, `null` for all; the guided tour shows a sample.
   */
  public readonly max = input<number | null>(null);

  /**
   * Whether to use the smaller type and spacing of a sample.
   */
  public readonly compact = input(false);

  /**
   * Translates each record's context tooltip.
   */
  private readonly translation = inject(Translation);

  /**
   * Records worth showing, in display order.
   */
  protected readonly tiles = computed<readonly RecordTile[]>(() => {
    const records = this.records();
    const tiles: RecordTile[] = [];

    // Grouped like everywhere else: a best game runs into five figures.
    const language = this.translation.language();
    const groupedScore = (value: number | null): string =>
      Number.isFinite(value) ? formatDamage(Math.round(value as number), language) : '—';

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

    const max = this.max();

    return max === null ? tiles : tiles.slice(0, max);
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
        date: formatLocalDayMonth(entry.achievedAt, this.translation.language()),
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
