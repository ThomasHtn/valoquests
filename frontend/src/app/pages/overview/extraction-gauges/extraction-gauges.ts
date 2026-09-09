import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
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
import { BLOCK_TOOLTIP_DELAY_MS, Tooltip } from '@shared/tooltip/tooltip';
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
    Tooltip,
  ],
  templateUrl: './extraction-gauges.html',
  styleUrl: './extraction-gauges.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExtractionGauges {
  /**
   * The four dials, or `null` outside a week in progress.
   */
  public readonly capacity = input.required<Capacity | null>();

  /**
   * The week's number, worded into the breakthrough dial as `Boss 04` rather than the
   * guardian's own name.
   */
  public readonly weekIndex = input.required<number>();

  protected readonly carryModes = CARRY_MODES;

  protected readonly shelterModes = SHELTER_MODES;

  private readonly translation = inject(Translation);

  /**
   * `Boss 04`, padded like the frieze's own week labels.
   */
  protected readonly bossLabel = computed(() =>
    this.translation.translate('overview.report.boss', {
      index: String(this.weekIndex()).padStart(2, '0'),
    }),
  );

  /**
   * What the tooltip hung off each ring waits before opening: the ring sits inside a card a reader
   * scans past, so it gets the same grace period as the other whole-block tooltips rather than
   * flashing on every pass.
   */
  protected readonly tooltipDelay = BLOCK_TOOLTIP_DELAY_MS;

  /**
   * How the "capacité d'emport" dial is worked out, read on hover: the mechanic a raw percentage
   * cannot carry on its own.
   */
  protected readonly carryTooltip = computed(() =>
    this.translation.translate('overview.capacity.carryTooltip', {
      rate: this.capacity()?.componentsPerRescue ?? 0,
    }),
  );

  protected readonly shelterTooltip = computed(() =>
    this.translation.translate('overview.capacity.shelterTooltip', {
      rate: this.capacity()?.foodPerRescue ?? 0,
    }),
  );

  protected readonly breachTooltip = computed(() =>
    this.translation.translate('overview.capacity.breachTooltip'),
  );

  protected readonly aboardTooltip = computed(() =>
    this.translation.translate('overview.capacity.aboardTooltip'),
  );

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  protected percent(fraction: number): number {
    return Math.round(fraction * 100);
  }
}
