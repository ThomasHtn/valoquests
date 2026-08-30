import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LucideChevronRight, LucideHammer } from '@lucide/angular';
import { interval } from 'rxjs';

import { BossApi } from '@core/boss/boss-api';
import { CurrentChallenges } from '@core/challenges/challenge.model';
import { formatDamage, formatSquadMultiplier } from '@core/challenges/challenge-format.utils';
import { resolveChallengeVisual } from '@core/challenges/challenge-visual.utils';
import { ChallengesApi } from '@core/challenges/challenges-api';
import { COUNTDOWN_REFRESH_INTERVAL_MS } from '@core/date/countdown.constants';
import {
  formatDayMonth,
  isoWeekNumber,
  nextWeekStart,
  RemainingTime,
  remainingWeekTime,
} from '@core/date/week-period.utils';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { PageHeader } from '@layout/page-header/page-header';
import { BossFightCard } from '@shared/boss-fight-card/boss-fight-card';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { WeekCountdown } from '@shared/week-countdown/week-countdown';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';
import { CatalogueRow, ChallengeRow } from './week.model';

/**
 * The week.
 *
 * Fusion of the old `/challenges` board and the boss card that used to lead the accueil (see
 * design-review.md §3.2): the boss the squad is fighting this week, as the hero-sized
 * `BossFightCard` shared with the accueil's own "La semaine" block, and the five challenges drawn
 * for it — each now showing how many of the squad has already cleared it. Who exactly stays
 * `Leaderboard`'s own matrix.
 */
@Component({
  selector: 'app-week',
  imports: [
    TranslatePipe,
    BossFightCard,
    LucideChevronRight,
    LucideHammer,
    PageHeader,
    ResourceState,
    WeekCountdown,
  ],
  templateUrl: './week.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Week {
  /**
   * Data-access service backing the shared current-challenges resource.
   */
  private readonly challengesApi = inject(ChallengesApi);

  /**
   * Data-access service backing the shared current-boss resource.
   */
  private readonly bossApi = inject(BossApi);

  /**
   * i18n service, read for the active language rather than for a lookup: the damage amounts are
   * grouped with the separator that language uses.
   */
  private readonly translation = inject(Translation);

  /**
   * Current time, refreshed periodically to keep the countdown display accurate.
   */
  private readonly now = signal(new Date());

  /**
   * Reactive resource fetching the current week's challenges, shared with the accueil header.
   */
  protected readonly challengesResource = this.challengesApi.current;

  /**
   * Reactive resource fetching the active week's boss confrontation.
   */
  protected readonly bossResource = this.bossApi.current;

  /**
   * Whether any backing resource is still loading.
   */
  protected readonly isLoading = anyLoading(this.challengesResource, this.bossResource);

  /**
   * Whether any backing resource failed to load.
   */
  protected readonly hasError = anyError(this.challengesResource, this.bossResource);

  /**
   * Placeholder line widths driving the loading skeleton.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * The active week as the backend describes it, or `null` until it has loaded.
   */
  private readonly currentWeek = computed<CurrentChallenges | null>(
    () => resourceValue(this.challengesResource, null) ?? null,
  );

  /**
   * The active week's boss confrontation, or `null` until it has loaded — read only to gate the
   * boss section's header, the fight itself is `BossFightCard`'s own concern.
   */
  protected readonly boss = computed(() => resourceValue(this.bossResource, null));

  /**
   * Active week's ISO number, or `null` while loading.
   */
  protected readonly weekNumber = computed<number | null>(() => {
    const currentWeek = this.currentWeek();
    return currentWeek === null ? null : isoWeekNumber(currentWeek.weekStart);
  });

  /**
   * Time left before the weekly rollover, or `null` while loading.
   */
  protected readonly remaining = computed<RemainingTime | null>(() => {
    const currentWeek = this.currentWeek();
    return currentWeek === null ? null : remainingWeekTime(currentWeek.weekEnd, this.now());
  });

  /**
   * Day the next five challenges are drawn, as `DD/MM`, or `null` while loading.
   */
  protected readonly nextDrawDate = computed<string | null>(() => {
    const currentWeek = this.currentWeek();
    return currentWeek === null ? null : formatDayMonth(nextWeekStart(currentWeek.weekEnd));
  });

  /**
   * The week's challenges, paired with their resolved tier and color treatment.
   */
  protected readonly rows = computed<readonly ChallengeRow[]>(() => {
    const language = this.translation.language();

    return (this.currentWeek()?.challenges ?? []).map((challenge) => ({
      id: challenge.id,
      name: challenge.name,
      description: challenge.description,
      difficulty: challenge.difficulty,
      damage: formatDamage(challenge.damage, language),
      materials: formatDamage(challenge.materials, language),
      squadMultiplier: formatSquadMultiplier(challenge.teamBonusPercent, language),
      visual: resolveChallengeVisual(challenge.metric, challenge.difficulty),
    }));
  });

  /**
   * The rest of the pool: every enabled challenge not among this week's five, outside of any
   * week's draw — what the group could still find itself facing another Monday.
   */
  protected readonly catalogueRows = computed<readonly CatalogueRow[]>(() => {
    const language = this.translation.language();
    const drawnIds = new Set(
      (this.currentWeek()?.challenges ?? []).map((challenge) => challenge.id),
    );

    return (resourceValue(this.challengesApi.catalogue, null)?.challenges ?? [])
      .filter((entry) => !drawnIds.has(entry.id))
      .map((entry) => ({
        id: entry.id,
        name: entry.name,
        description: entry.description,
        difficulty: entry.difficulty,
        damage: formatDamage(entry.damage, language),
        materials: formatDamage(entry.materials, language),
        visual: resolveChallengeVisual(entry.metric, entry.difficulty),
      }));
  });

  /**
   * Refreshes {@link now} every minute so the countdown stays accurate for the lifetime of the
   * page.
   */
  constructor() {
    interval(COUNTDOWN_REFRESH_INTERVAL_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.now.set(new Date()));
  }

  /**
   * Reloads every backing resource after a failure.
   */
  protected reload(): void {
    reloadAll(this.challengesResource, this.bossResource);
  }
}
