import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideFlame,
  LucideRefreshCw,
  LucideTarget,
  LucideWheat,
  LucideWrench,
} from '@lucide/angular';

import { WEEKLY_TITLES, WeeklyTitle } from '@core/campaign/campaign.model';
import { resolveTitleVisual, TitleVisual } from '@core/campaign/campaign-visual.utils';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { PlayersApi } from '@core/players/players-api';

/**
 * A title on the contribution block: how it is drawn, and how many times it was earned.
 */
export interface ContributionTitle extends TitleVisual {
  readonly key: WeeklyTitle;
  readonly count: number;
}

/**
 * One figure of a contribution block: its caption, its value, the colour of what it counts.
 */
export interface ContributionCell {
  readonly key: string;
  readonly value: string;
  readonly note: string;
  readonly tone: string;
}

/**
 * What one operator brought, at the two scales that count: the week's ranking and the campaign.
 *
 * Heads the profile, above the Valorant statistics: the profile used to open on a rank the
 * squad's game never reads, and what the game does read was nowhere on it.
 */
@Component({
  selector: 'app-player-contribution',
  imports: [
    TranslatePipe,
    NgTemplateOutlet,
    RouterLink,
    LucideFlame,
    LucideRefreshCw,
    LucideTarget,
    LucideWheat,
    LucideWrench,
  ],
  templateUrl: './contribution.html',
  styleUrl: './contribution.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlayerContribution {
  public readonly playerId = input.required<number>();

  private readonly playersApi = inject(PlayersApi);

  private readonly translation = inject(Translation);

  protected readonly resource = this.playersApi.contribution(this.playerId);

  protected readonly contribution = computed(() => resourceValue(this.resource, null) ?? null);

  protected readonly week = computed(() => this.contribution()?.week ?? null);

  protected readonly campaign = computed(() => this.contribution()?.campaign ?? null);

  protected readonly weekCells = computed<readonly ContributionCell[]>(() => {
    const week = this.week();
    if (!week) {
      return [];
    }
    const t = (key: string, params?: Record<string, string | number>): string =>
      this.translation.translate(`playerProfile.contribution.${key}`, params);
    return [
      this.cell('damage', this.format(week.guardianDamage), '', 'var(--color-boss-hp-edge)'),
      this.cell(
        'points',
        this.format(week.challengePoints),
        `${t('weekly', { count: week.completedChallenges })} · ${t('daily', { count: week.completedDailyChallenges })}`,
        'var(--color-accent-blue)',
      ),
      this.cell(
        'streak',
        t('streakDays', { count: week.streakDays }),
        '',
        'var(--color-brand-400)',
      ),
      this.cell('days', `${week.activeDays}`, t('daysOf', { count: 7 }), ''),
      this.cell('matches', `${week.matchCount}`, '', ''),
    ];
  });

  protected readonly campaignCells = computed<readonly ContributionCell[]>(() => {
    const campaign = this.campaign();
    if (!campaign) {
      return [];
    }
    const t = (key: string, params?: Record<string, string | number>): string =>
      this.translation.translate(`playerProfile.contribution.${key}`, params);
    return [
      this.cell('damage', this.format(campaign.damage), '', 'var(--color-boss-hp-edge)'),
      this.cell('components', this.format(campaign.components), '', 'var(--color-accent-cyan)'),
      this.cell('food', this.format(campaign.food), '', 'var(--color-accent-green)'),
      this.cell(
        'rescued',
        this.format(campaign.survivorsRescued),
        t('rescuedNote', { count: campaign.completedChallenges }),
        'var(--color-brand-400)',
      ),
      this.cell('blows', `${campaign.finishingBlows}`, '', ''),
      this.cell(
        'longestStreak',
        t('streakDays', { count: campaign.longestStreak }),
        t('daysPlayed', { count: campaign.activeDays }),
        '',
      ),
      this.cell('matches', `${campaign.matchCount}`, '', ''),
    ];
  });

  protected readonly weekTitles = computed<readonly ContributionTitle[]>(
    () => this.week()?.titles.map((key) => ({ key, count: 1, ...resolveTitleVisual(key) })) ?? [],
  );

  /**
   * Every title earned over the campaign, in the ruleset's order, with how many times.
   */
  protected readonly campaignTitles = computed<readonly ContributionTitle[]>(() => {
    const titles = this.campaign()?.titles ?? {};
    return WEEKLY_TITLES.filter((key) => (titles[key] ?? 0) > 0).map((key) => ({
      key,
      count: titles[key] ?? 0,
      ...resolveTitleVisual(key),
    }));
  });

  private cell(key: string, value: string, note: string, tone: string): ContributionCell {
    return { key, value, note, tone };
  }

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }
}
