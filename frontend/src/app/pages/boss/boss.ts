import { Component, computed, effect, ElementRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';

import { BossApi } from '@core/boss/boss-api';
import { BossHistoryWeek, CurrentBoss } from '@core/boss/boss.model';
import { resolveBossCategoryColorClass } from '@core/boss/boss-visual.utils';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { COUNTDOWN_REFRESH_INTERVAL_MS } from '@core/date/countdown.constants';
import {
  addDays,
  formatDateRange,
  isoWeekNumber,
  remainingWeekTime,
} from '@core/date/week-period.utils';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SectionDivider } from '@shared/section-divider/section-divider';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import { BossDetail } from './boss-detail/boss-detail';
import {
  resolveBossDamageBarLabelKey,
  resolveBossStatusLabelKey,
  resolveBossTimelineTier,
} from './boss-timeline.constants';
import { BossContribution, BossTimelineNode } from './boss.model';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * Number of locked placeholder markers rendered after the current week, representing weeks ahead
 * whose boss doesn't exist yet — the backend only ever draws a week's boss lazily, once that week
 * becomes current (see `DefaultWeeklyBossSelectionService`), so there is no real data to show for
 * them.
 */
const UPCOMING_PLACEHOLDER_COUNT = 3;

/**
 * Campaign page: the group's whole run of weekly boss confrontations.
 *
 * A centered vertical timeline from the oldest finalized week to the active one and a handful of
 * locked weeks ahead, auto-scrolled to the current week's marker on load. Selecting any node opens
 * a detail panel breaking that week's damage down per player.
 */
@Component({
  selector: 'app-boss',
  imports: [TranslatePipe, BossDetail, ResourceState, SectionDivider],
  templateUrl: './boss.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Boss {
  /**
   * Data-access service backing the boss history and current-week resources.
   */
  private readonly bossApi = inject(BossApi);

  /**
   * Data-access service backing the shared players resource, used to resolve contributor avatars.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * Data-access service backing the weekly rankings, which is where a week's damage breakdown per
   * player comes from (see {@link contributionsByWeekStart}).
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * i18n service used to resolve every translated label baked into a timeline node.
   */
  private readonly translation = inject(Translation);

  /**
   * Host element, queried once to scroll the current week's marker into view on load.
   */
  private readonly hostElement = inject(ElementRef<HTMLElement>);

  /**
   * Reactive resource fetching every tracked player's summary, used to resolve avatars.
   */
  private readonly playersResource = this.playersApi.players;

  /**
   * Reactive resource fetching the active week's boss confrontation.
   */
  protected readonly currentResource = this.bossApi.current;

  /**
   * Reactive resource fetching every finalized week of boss history in one request.
   */
  protected readonly historyResource = this.bossApi.history;

  /**
   * Reactive resources fetching the rankings the damage breakdown is reconstructed from: the
   * active week's, and every finalized week's.
   */
  private readonly currentRankingResource = this.rankingApi.current;
  private readonly rankingHistoryResource = this.rankingApi.history;

  /**
   * Current time, refreshed periodically to keep the active week's countdown accurate.
   */
  private readonly now = signal(new Date());

  /**
   * Id of the node whose detail panel is open, or `null` while the panel is closed.
   */
  private readonly selectedNodeId = signal<string | null>(null);

  /**
   * Whether the current week's marker has already been scrolled into view, so the one-time
   * auto-scroll effect never fires twice for the same load.
   */
  private hasScrolledToCurrentNode = false;

  /**
   * Whether any backing resource is still loading.
   */
  protected readonly isLoading = anyLoading(
    this.historyResource,
    this.currentResource,
    this.playersResource,
    this.currentRankingResource,
    this.rankingHistoryResource,
  );

  /**
   * Whether any backing resource failed to load.
   */
  protected readonly hasError = anyError(
    this.historyResource,
    this.currentResource,
    this.playersResource,
    this.currentRankingResource,
    this.rankingHistoryResource,
  );

  /**
   * Avatar URL per player id, resolved from the shared players resource.
   */
  private readonly avatarUrlByPlayerId = computed(
    () =>
      new Map(
        resourceValue(this.playersResource, []).map(
          (player) => [player.id, resolvePlayerAvatarUrl(player.portrait)] as const,
        ),
      ),
  );

  /**
   * Id of the reigning weekly "Champion", or `null` while unknown or before any week has been
   * finalized.
   *
   * Read off the full ranking history this page already fetches — its first entry *is* the most
   * recently finalized week — rather than off `RankingApi.latestFinalizedWeek`, which would cost
   * the page a second request for the same week.
   */
  private readonly championPlayerId = computed(() =>
    resolveChampionPlayerId(resourceValue(this.rankingHistoryResource, null)),
  );

  /**
   * Each week's damage broken down per player, keyed by the week's ISO `weekStart`.
   *
   * A player's challenge damage for a week is exactly the damage that week's boss took from them,
   * so the rankings already hold the breakdown and no dedicated endpoint is needed. The active
   * week comes from the live ranking (which additionally knows how many challenges the week has,
   * hence the `4/5` wording there); finalized weeks come from the ranking history.
   */
  private readonly contributionsByWeekStart = computed(() => {
    const avatarUrlByPlayerId = this.avatarUrlByPlayerId();
    const championPlayerId = this.championPlayerId();
    const language = this.translation.language();

    const byWeekStart = new Map<string, readonly BossContribution[]>();

    for (const week of resourceValue(this.rankingHistoryResource, null)?.content ?? []) {
      byWeekStart.set(
        week.weekStart,
        week.ranking.map((entry) => ({
          playerId: entry.playerId,
          position: entry.position,
          displayName: entry.displayName,
          avatarUrl: avatarUrlByPlayerId.get(entry.playerId) ?? null,
          isChampion: entry.playerId === championPlayerId,
          damageLabel: formatDamage(entry.challengeDamage, language),
          questsLabel: `${entry.completedChallenges}`,
        })),
      );
    }

    const currentRanking = resourceValue(this.currentRankingResource, null);
    if (currentRanking) {
      byWeekStart.set(
        currentRanking.weekStart,
        currentRanking.ranking.flatMap((entry) => {
          // Inactive players hold no position — they never consume a ranking slot — and dealt the
          // boss no damage either, so they are left out of the breakdown rather than listed at a
          // rank they don't have. The `typeof` guard also covers the position being absent from
          // the payload altogether, which is how the backend serializes it.
          const position = entry.position;
          if (typeof position !== 'number') {
            return [];
          }

          return [
            {
              playerId: entry.player.id,
              position,
              displayName: entry.player.displayName,
              avatarUrl: avatarUrlByPlayerId.get(entry.player.id) ?? null,
              isChampion: entry.player.id === championPlayerId,
              damageLabel: formatDamage(entry.challengeDamage, language),
              questsLabel: `${entry.completedChallenges}/${entry.totalChallenges}`,
            },
          ];
        }),
      );
    }

    return byWeekStart;
  });

  /**
   * The full campaign timeline: every finalized week oldest-first, the active week, then a fixed
   * number of locked placeholders for weeks ahead.
   */
  protected readonly nodes = computed<readonly BossTimelineNode[]>(() => {
    const contributionsByWeekStart = this.contributionsByWeekStart();

    const historyNodes = [...(resourceValue(this.historyResource, null)?.content ?? [])]
      .reverse()
      .map((week) => this.toHistoryNode(week, contributionsByWeekStart.get(week.weekStart) ?? []));

    const currentBoss = resourceValue(this.currentResource, null);
    const currentNode = currentBoss
      ? [this.toCurrentNode(currentBoss, contributionsByWeekStart.get(currentBoss.weekStart) ?? [])]
      : [];

    const upcomingNodes = Array.from({ length: UPCOMING_PLACEHOLDER_COUNT }, (_, index) =>
      this.toUpcomingNode(index, currentBoss?.weekEnd ?? null),
    );

    return [...historyNodes, ...currentNode, ...upcomingNodes];
  });

  /**
   * Campaign tally shown on the divider under the header: bosses put down, bosses faced, and the
   * damage the group has dealt across the whole run.
   */
  protected readonly progressLabel = computed(() => {
    const finalizedWeeks = resourceValue(this.historyResource, null)?.content ?? [];
    const currentBoss = resourceValue(this.currentResource, null);

    const totalDamage =
      finalizedWeeks.reduce((total, week) => total + week.totalDamageDealt, 0) +
      (currentBoss?.totalDamageDealt ?? 0);

    return this.translation.translate('boss.progress', {
      defeated: finalizedWeeks.filter((week) => week.defeated).length,
      fought: finalizedWeeks.length + (currentBoss ? 1 : 0),
      damage: formatDamage(totalDamage, this.translation.language()),
    });
  });

  /**
   * The node whose detail panel is open, or `null` while the panel is closed.
   */
  protected readonly selectedNode = computed<BossTimelineNode | null>(
    () => this.nodes().find((node) => node.id === this.selectedNodeId()) ?? null,
  );

  /**
   * Position of {@link selectedNode} within the timeline, or `-1` while the panel is closed.
   */
  private readonly selectedIndex = computed(() =>
    this.nodes().findIndex((node) => node.id === this.selectedNodeId()),
  );

  /**
   * Whether the panel can step to an earlier / later week without leaving the timeline.
   */
  protected readonly hasPreviousNode = computed(() => this.selectedIndex() > 0);
  protected readonly hasNextNode = computed(() => {
    const index = this.selectedIndex();
    return index >= 0 && index < this.nodes().length - 1;
  });

  /**
   * Placeholder line widths driving the loading skeleton.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * Resolves a timeline node's visual tier (marker, panel tint, pill, bar), exposed to the
   * template.
   */
  protected readonly timelineTier = resolveBossTimelineTier;

  /**
   * Background gradient for the timeline's center line: ground already covered in brand amber,
   * turning red at the active week's position, then flat and muted ahead — a progress readout for
   * the whole campaign at a glance, without measuring every row's rendered height to color each
   * segment individually. The same three colors the markers themselves use, in the same order.
   */
  protected readonly timelineLineGradient = computed(() => {
    const nodes = this.nodes();
    const currentIndex = nodes.findIndex((node) => node.status === 'current');
    const progressPercentage =
      nodes.length > 1 && currentIndex >= 0
        ? Math.round((currentIndex / (nodes.length - 1)) * 100)
        : 0;

    return (
      `linear-gradient(to bottom, var(--color-brand-500) 0%, ` +
      `var(--color-accent-red) ${progressPercentage}%, ` +
      `var(--color-surface-700) ${progressPercentage}%, var(--color-surface-800) 100%)`
    );
  });

  /**
   * Scrolls the current week's marker into view once, the first time the timeline finishes
   * loading — the page's one-time "you are here" cue on a potentially long campaign. Refreshes
   * {@link now} every minute so the active week's countdown stays accurate for the lifetime of the
   * page.
   *
   * `block: 'nearest'` rather than `'center'`: centering can require enough scroll to push the
   * page's own `<header>` half off-screen (clipped, not fully hidden) on short mobile viewports,
   * since it scrolls above the marker in the same document flow. `'nearest'` still scrolls when
   * the marker starts off-screen, just without overshooting past a fully-visible result.
   * `requestAnimationFrame` is a browser-only API, safe to call unconditionally here since this
   * effect only ever runs client-side, after `isLoading` first turns false.
   */
  constructor() {
    effect(() => {
      if (this.hasScrolledToCurrentNode || this.isLoading()) {
        return;
      }

      this.hasScrolledToCurrentNode = true;
      requestAnimationFrame(() => {
        this.hostElement.nativeElement
          .querySelector('[data-timeline-current]')
          ?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      });
    });

    interval(COUNTDOWN_REFRESH_INTERVAL_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.now.set(new Date()));
  }

  /**
   * Opens the detail panel on one week.
   *
   * @param node - The timeline node to detail.
   */
  protected select(node: BossTimelineNode): void {
    this.selectedNodeId.set(node.id);
  }

  /**
   * Steps the open panel to the adjacent week, if there is one in that direction.
   *
   * @param offset - `-1` for the previous week, `1` for the next one.
   */
  protected step(offset: -1 | 1): void {
    const target = this.nodes()[this.selectedIndex() + offset];
    if (target) {
      this.selectedNodeId.set(target.id);
    }
  }

  /**
   * Closes the detail panel.
   */
  protected closePanel(): void {
    this.selectedNodeId.set(null);
  }

  /**
   * Reloads every backing resource after a failure.
   */
  protected reload(): void {
    this.hasScrolledToCurrentNode = false;
    reloadAll(
      this.historyResource,
      this.currentResource,
      this.playersResource,
      this.currentRankingResource,
      this.rankingHistoryResource,
    );
  }

  /**
   * Maps one finalized week to its timeline node.
   *
   * @param week - Finalized boss confrontation.
   * @param contributions - That week's damage broken down per player.
   * @returns The display-ready timeline node.
   */
  private toHistoryNode(
    week: BossHistoryWeek,
    contributions: readonly BossContribution[],
  ): BossTimelineNode {
    const status = week.defeated ? 'defeated' : 'survived';
    const topContributor = contributions[0];

    return {
      ...this.toFoughtNode(
        week.weekStart,
        week.weekEnd,
        status,
        week.boss,
        week.effectiveHp,
        week.totalDamageDealt,
        contributions,
      ),
      metaLabel: week.defeated
        ? week.defeatedByPlayerDisplayName === null
          ? null
          : this.translation.translate('boss.meta.finishingBlow', {
              player: week.defeatedByPlayerDisplayName,
            })
        : topContributor
          ? this.translation.translate('boss.meta.topDamage', {
              player: topContributor.displayName,
              damage: topContributor.damageLabel,
            })
          : null,
    };
  }

  /**
   * Maps the active week's boss confrontation to its timeline node.
   *
   * @param current - Active week's boss confrontation.
   * @param contributions - The week's damage broken down per player so far.
   * @returns The display-ready timeline node.
   */
  private toCurrentNode(
    current: CurrentBoss,
    contributions: readonly BossContribution[],
  ): BossTimelineNode {
    const remaining = remainingWeekTime(current.weekEnd, this.now());

    return {
      ...this.toFoughtNode(
        current.weekStart,
        current.weekEnd,
        'current',
        current.boss,
        current.effectiveHp,
        current.totalDamageDealt,
        contributions,
      ),
      metaLabel: this.translation.translate('boss.meta.remaining', {
        days: remaining.days,
        hours: remaining.hours,
      }),
    };
  }

  /**
   * Builds everything a week with a drawn boss shares, whatever its outcome — identity, dates and
   * the damage readout — leaving each caller to add only its own meta line.
   *
   * @param weekStart - Monday identifying the week, as `YYYY-MM-DD`.
   * @param weekEnd - Sunday identifying the week, as `YYYY-MM-DD`.
   * @param status - The week's outcome/state.
   * @param boss - The boss drawn for that week.
   * @param effectiveHp - The boss's hit points once its difficulty modifier is applied.
   * @param totalDamageDealt - Damage the group dealt it.
   * @param contributions - That damage broken down per player.
   * @returns The display-ready node, with a `null` meta line.
   */
  private toFoughtNode(
    weekStart: string,
    weekEnd: string,
    status: 'defeated' | 'survived' | 'current',
    boss: BossHistoryWeek['boss'],
    effectiveHp: number,
    totalDamageDealt: number,
    contributions: readonly BossContribution[],
  ): BossTimelineNode {
    const language = this.translation.language();
    const percentage =
      effectiveHp > 0 ? Math.min(100, Math.round((totalDamageDealt / effectiveHp) * 100)) : 0;

    return {
      id: weekStart,
      status,
      weekNumber: isoWeekNumber(weekStart),
      weekLabel: this.translation.translate('boss.week.label', {
        number: isoWeekNumber(weekStart),
      }),
      dateRangeLabel: formatDateRange(weekStart, weekEnd),
      statusLabel: this.translation.translate(resolveBossStatusLabelKey(status)),
      bossName: boss.name,
      bossDescription: boss.description,
      categoryLabel: this.translation.translate(`boss.category.${boss.category}`),
      categoryColorClass: resolveBossCategoryColorClass(boss.category),
      portraitUrl: boss.imageUrl,
      hasDamage: true,
      damagePercentage: percentage,
      damagePercentageLabel: this.translation.translate('boss.damagePercentage', {
        value: percentage,
      }),
      damageLabel: this.translation.translate('boss.damageDealt', {
        damage: formatDamage(totalDamageDealt, language),
        hp: formatDamage(effectiveHp, language),
      }),
      barLabel: this.translation.translate(resolveBossDamageBarLabelKey(status)),
      metaLabel: null,
      contributions,
    };
  }

  /**
   * Builds one locked placeholder node for a week ahead with no boss drawn yet.
   *
   * The week itself is known even though its boss isn't — the calendar runs on fixed Monday-to-
   * Sunday periods — so the marker still carries its number and dates, and only the opponent stays
   * sealed. They fall back to blank in the one case the anchor is missing: the active week failing
   * to load, which leaves nothing to count forward from.
   *
   * @param index - Zero-based position among the upcoming placeholders.
   * @param currentWeekEnd - The active week's end date as `YYYY-MM-DD`, or `null` when unknown.
   * @returns The display-ready timeline node.
   */
  private toUpcomingNode(index: number, currentWeekEnd: string | null): BossTimelineNode {
    const weekStart = currentWeekEnd === null ? null : addDays(currentWeekEnd, 1 + index * 7);

    return {
      id: `upcoming-${index}`,
      status: 'upcoming',
      weekNumber: weekStart === null ? null : isoWeekNumber(weekStart),
      weekLabel:
        weekStart === null
          ? null
          : this.translation.translate('boss.week.label', { number: isoWeekNumber(weekStart) }),
      dateRangeLabel: weekStart === null ? null : formatDateRange(weekStart, addDays(weekStart, 6)),
      statusLabel: this.translation.translate(resolveBossStatusLabelKey('upcoming')),
      bossName: this.translation.translate('boss.upcoming.name'),
      bossDescription: this.translation.translate('boss.upcoming.description'),
      categoryLabel: null,
      categoryColorClass: null,
      portraitUrl: null,
      hasDamage: false,
      damagePercentage: 0,
      damagePercentageLabel: '',
      damageLabel: '',
      barLabel: '',
      metaLabel: null,
      contributions: [],
    };
  }
}
