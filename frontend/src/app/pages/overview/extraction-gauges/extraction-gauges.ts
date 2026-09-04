import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import {
  LucideBuilding2,
  LucideRocket,
  LucideSkull,
  LucideSwords,
  LucideUsers,
  LucideWheat,
  LucideWrench,
} from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Capacity } from '../overview.model';

/**
 * Game modes that feed each stock the most, as the scoring splits a match's value.
 */
const CARRY_MODES: readonly string[] = ['COMPETITIVE', 'PREMIER', 'UNRATED'];
const SHELTER_MODES: readonly string[] = [
  'DEATHMATCH',
  'SPIKE_RUSH',
  'TEAM_DEATHMATCH',
  'SKIRMISH',
];

/**
 * What would come home on Sunday, and the three things that bound it: four dials on the same
 * scale, the wounded spotted — the three limits, then what gets through.
 *
 * Under each name, the raw quantity that produces the dial: a player who only sees the conversion
 * cannot decide to save up, which is the one decision the game asks of them.
 */
@Component({
  selector: 'app-extraction-gauges',
  imports: [
    LowerCasePipe,
    TranslatePipe,
    LucideBuilding2,
    LucideRocket,
    LucideSkull,
    LucideSwords,
    LucideUsers,
    LucideWheat,
    LucideWrench,
  ],
  templateUrl: './extraction-gauges.html',
  styleUrl: './extraction-gauges.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExtractionGauges {
  /**
   * The four dials, or `null` outside a week in progress.
   */
  public readonly capacity = input.required<Capacity | null>();

  /**
   * Name of the week's guardian, worded into the breakthrough dial.
   */
  public readonly guardianName = input.required<string>();

  protected readonly carryModes = CARRY_MODES;

  protected readonly shelterModes = SHELTER_MODES;

  private readonly translation = inject(Translation);

  /**
   * The guardian's first name, before any epithet: "Kharn" rather than "Kharn, the watcher of
   * the dunes", which the dial has no room for.
   */
  protected guardianShortName(): string {
    return this.guardianName().split(',')[0].trim();
  }

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  protected percent(fraction: number): number {
    return Math.round(fraction * 100);
  }
}
