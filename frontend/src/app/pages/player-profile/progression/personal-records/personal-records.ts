import { Component, computed, inject, input } from '@angular/core';
import {
  LucideCalendarCheck,
  LucideCrosshair,
  LucideFlame,
  LucideShieldCheck,
  LucideStar,
  LucideSwords,
  LucideTarget,
  LucideTrendingUp,
  LucideZap,
} from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { formatLocalDayMonth } from '@core/date/date-time.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolveCompetitiveTierVisual } from '@core/players/competitive-tier.utils';
import {
  formatHeadshotPercentage,
  formatKda,
  formatScore,
} from '@core/players/player-format.utils';
import {
  PersonalRecords as PersonalRecordsData,
  RecordEntry,
} from '@core/players/player-progression.model';
import { Tooltip } from '@shared/tooltip/tooltip';

/**
 * Every record the section can show, in display order. Doubles as the translation key suffix.
 */
type RecordKey =
  | 'mostKills'
  | 'bestAcs'
  | 'mostDamage'
  | 'bestKda'
  | 'bestHeadshotPercentage'
  | 'longestWinStreak'
  | 'longestActiveDayStreak'
  | 'mvps'
  | 'peakTier';

/**
 * One record, ready to render.
 */
interface RecordTile {
  /**
   * Which record this is; picks both the icon and the label.
   */
  readonly key: RecordKey;

  /**
   * The record itself, already formatted.
   */
  readonly value: string;

  /**
   * Already-translated explanation shown on the label, carrying where and when it was set.
   */
  readonly tooltip: string;
}

/**
 * A player's personal bests, as icon-and-figure tiles.
 *
 * Highs only, never lows. This section is read by the player it describes, and a "worst match"
 * tile would be nothing but somewhere to feel bad — so a record nobody has set yet is simply left
 * out rather than shown as a zero.
 */
@Component({
  selector: 'app-personal-records',
  imports: [
    TranslatePipe,
    Tooltip,
    LucideCrosshair,
    LucideZap,
    LucideFlame,
    LucideSwords,
    LucideTarget,
    LucideTrendingUp,
    LucideCalendarCheck,
    LucideStar,
    LucideShieldCheck,
  ],
  templateUrl: './personal-records.html',
})
export class PersonalRecords {
  /**
   * The player's records, as the API returned them.
   */
  public readonly records = input.required<PersonalRecordsData>();

  /**
   * How many records to show at most, or `null` for all of them.
   *
   * Exists for the guided tour, which shows this section as a sample of what a profile holds rather
   * than as the profile's own full grid.
   */
  public readonly max = input<number | null>(null);

  /**
   * Whether to render the tiles at the smaller type and spacing of a sample, rather than at the
   * profile's own scale.
   */
  public readonly compact = input(false);

  /**
   * i18n service, used for the tooltips carrying each record's context.
   */
  private readonly translation = inject(Translation);

  /**
   * The records worth showing, in display order.
   */
  protected readonly tiles = computed<readonly RecordTile[]>(() => {
    const records = this.records();
    const tiles: RecordTile[] = [];

    // Grouped, unlike the three-digit scores beside it: a best game runs into five figures, and
    // `6254` sat under a tile that reads `6 254` on every other screen of the application.
    const language = this.translation.language();
    const groupedScore = (value: number | null): string =>
      Number.isFinite(value) ? formatDamage(Math.round(value as number), language) : '—';

    this.pushMatchRecord(tiles, 'mostKills', records.mostKills, (value) => String(value));
    this.pushMatchRecord(tiles, 'bestAcs', records.bestAcs, formatScore);
    this.pushMatchRecord(tiles, 'mostDamage', records.mostDamage, groupedScore);
    this.pushMatchRecord(tiles, 'bestKda', records.bestKda, formatKda);
    this.pushMatchRecord(
      tiles,
      'bestHeadshotPercentage',
      records.bestHeadshotPercentage,
      formatHeadshotPercentage,
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
      tiles.push(this.simpleTile('peakTier', tier.label));
    }

    const max = this.max();

    return max === null ? tiles : tiles.slice(0, max);
  });

  /**
   * Appends a per-match record, unless no match ever set it.
   *
   * @param tiles - Tiles being assembled.
   * @param key - Which record this is.
   * @param entry - The record and the match it was set in, or `null`.
   * @param format - Formats the record's figure.
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
    });
  }

  /**
   * Builds a record that stands on its own, with no single match behind it.
   *
   * @param key - Which record this is.
   * @param value - The record, already formatted.
   * @returns The tile.
   */
  private simpleTile(key: RecordKey, value: string): RecordTile {
    return {
      key,
      value,
      tooltip: this.translation.translate(`playerProfile.progression.records.tooltip.${key}`),
    };
  }
}
