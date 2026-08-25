import { computed, inject, Service, signal, Signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';

import { BossApi } from '@core/boss/boss-api';
import { BossTimelineNode, BossContribution } from '@core/boss/boss-timeline.model';
import { BossHistoryWeek, CurrentBoss } from '@core/boss/boss.model';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { COUNTDOWN_REFRESH_INTERVAL_MS } from '@core/date/countdown.constants';
import {
  addDays,
  formatDateRange,
  isoWeekNumber,
  remainingWeekTime,
} from '@core/date/week-period.utils';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { Translation } from '@core/i18n/translation';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { resolveBossHpBarLabelKey, resolveBossStatusLabelKey } from './boss-timeline.constants';

/**
 * Number of locked placeholder nodes appended when the run's length is unknown.
 *
 * Only reached while the active week has failed to load, which is also the one case there is
 * nothing to count forward from. The campaign otherwise pads itself to the run's own length (see
 * {@link BossCampaign.nodes}): a run is exactly ten weekly rollovers, so the map has a fixed size
 * from the moment it opens — the property the Valorant act it replaces never had.
 */
const FALLBACK_PLACEHOLDER_COUNT = 3;

/**
 * Damage one player dealt to a week's boss, from their ranking entry.
 *
 * Their weekly total minus the regularity bonus, which is the one component that stays out of the
 * fight — it rewards showing up rather than output. This is the same subtraction
 * `DefaultBossQueryService#totalDamageDealt` makes to fill the health bar, so a week's rows add up
 * to the bar they sit under.
 *
 * @param entry - The player's ranking entry, live or finalized.
 * @returns The damage that week's boss took from them.
 */
function bossDamageOf(entry: {
  readonly totalDamage: number;
  readonly regularityBonus: number;
}): number {
  return entry.totalDamage - entry.regularityBonus;
}

/**
 * The group's whole run of weekly boss confrontations, resolved once into display-ready nodes.
 *
 * Every label a node carries is baked here already translated and already formatted, so the pages
 * rendering the campaign only lay it out. Two do: the battle map (`Campaign`) and the legacy
 * timeline (`Boss`) — they draw the same run in two shapes, which is why the derivation lives in a
 * service rather than in either of them.
 *
 * Provided at component level rather than in the injector root, so {@link takeUntilDestroyed} does
 * cut the countdown ticker when the reader leaves the page. Sharing costs nothing: the underlying
 * `httpResource`s live in the api services and are shared regardless.
 */
@Service()
export class BossCampaign {
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
   * i18n service used to resolve every translated label baked into a node.
   */
  private readonly translation = inject(Translation);

  /**
   * Reactive resource fetching every tracked player's summary, used to resolve avatars.
   */
  private readonly playersResource = this.playersApi.players;

  /**
   * Reactive resource fetching the active week's boss confrontation.
   */
  private readonly currentResource = this.bossApi.current;

  /**
   * Reactive resource fetching every finalized week of boss history in one request.
   */
  private readonly historyResource = this.bossApi.history;

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
   * Whether any backing resource is still loading.
   */
  public readonly isLoading = anyLoading(
    this.historyResource,
    this.currentResource,
    this.playersResource,
    this.currentRankingResource,
    this.rankingHistoryResource,
  );

  /**
   * Whether any backing resource failed to load.
   */
  public readonly hasError = anyError(
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
   * Read off the full ranking history this service already fetches — its first entry *is* the most
   * recently finalized week — rather than off `RankingApi.latestFinalizedWeek`, which would cost a
   * second request for the same week.
   */
  private readonly championPlayerId = computed(() =>
    resolveChampionPlayerId(resourceValue(this.rankingHistoryResource, null)),
  );

  /**
   * Each week's damage to the boss, broken down per player, keyed by the week's ISO `weekStart`.
   *
   * The rankings already hold the breakdown, so no dedicated endpoint is needed — but a player's
   * ranking total is not what the boss took from them. The regularity bonus rewards showing up
   * rather than output and never reaches the shared health bar, so `DefaultBossQueryService` sums
   * `totalDamage - regularityBonus` and these rows have to subtract it too. They did not, which put
   * up to 6 000 of phantom damage per player per week in a list printed directly under a bar
   * computed without it.
   *
   * Reading the challenge damage alone would go the other way and leave the rows short of the bar,
   * and frozen whenever a player only scored match damage or a bonus. The active week comes from
   * the live ranking (which additionally knows how many challenges the week has, hence the `4/5`
   * wording there); finalized weeks come from the ranking history.
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
          damageLabel: formatDamage(bossDamageOf(entry), language),
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
              damageLabel: formatDamage(bossDamageOf(entry), language),
              questsLabel: `${entry.completedChallenges}/${entry.totalChallenges}`,
            },
          ];
        }),
      );
    }

    return byWeekStart;
  });

  /**
   * The full campaign: every finalized week oldest-first, the active week, then locked placeholders
   * up to the run's own length.
   *
   * The map is exactly as long as a run, whatever has been fought so far, because a run is ten
   * weekly rollovers rather than ten fights — a week can go by with no boss drawn at all, and
   * counting fights would make the map shrink and grow for reasons nobody could read.
   */
  public readonly nodes: Signal<readonly BossTimelineNode[]> = computed(() => {
    const contributionsByWeekStart = this.contributionsByWeekStart();

    const historyNodes = [...(resourceValue(this.historyResource, null)?.content ?? [])]
      .reverse()
      .map((week) => this.toHistoryNode(week, contributionsByWeekStart.get(week.weekStart) ?? []));

    const currentBoss = resourceValue(this.currentResource, null);
    const currentNode = currentBoss
      ? [this.toCurrentNode(currentBoss, contributionsByWeekStart.get(currentBoss.weekStart) ?? [])]
      : [];

    const upcomingCount = currentBoss
      ? Math.max(0, currentBoss.runWeekCount - historyNodes.length - currentNode.length)
      : FALLBACK_PLACEHOLDER_COUNT;

    const upcomingNodes = Array.from({ length: upcomingCount }, (_, index) =>
      this.toUpcomingNode(index, currentBoss?.weekEnd ?? null),
    );

    return [...historyNodes, ...currentNode, ...upcomingNodes];
  });

  /**
   * Position of the active week in {@link nodes}, or `-1` when no week is currently running.
   */
  public readonly currentNodeIndex = computed(() =>
    this.nodes().findIndex((node) => node.status === 'current'),
  );

  /**
   * Refreshes {@link now} every minute so the active week's countdown stays accurate for as long as
   * a page holding this service is on screen.
   */
  constructor() {
    interval(COUNTDOWN_REFRESH_INTERVAL_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.now.set(new Date()));
  }

  /**
   * Reloads every backing resource after a failure.
   */
  public reload(): void {
    reloadAll(
      this.historyResource,
      this.currentResource,
      this.playersResource,
      this.currentRankingResource,
      this.rankingHistoryResource,
    );
  }

  /**
   * Maps one finalized week to its node.
   *
   * @param week - Finalized boss confrontation.
   * @param contributions - That week's damage broken down per player.
   * @returns The display-ready node.
   */
  private toHistoryNode(
    week: BossHistoryWeek,
    contributions: readonly BossContribution[],
  ): BossTimelineNode {
    const status = week.defeated ? 'defeated' : 'survived';
    const topContributor = contributions[0];
    const finishingBlowLabel =
      week.defeatedByPlayerDisplayName === null
        ? null
        : this.translation.translate('boss.meta.finishingBlow', {
            player: week.defeatedByPlayerDisplayName,
          });

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
        ? finishingBlowLabel
        : topContributor
          ? this.translation.translate('boss.meta.topDamage', {
              player: topContributor.displayName,
              damage: topContributor.damageLabel,
            })
          : null,
      // The top-damage wording is dropped in the panel: it repeats the first row of the ranking the
      // panel already prints underneath. The finishing blow isn't in that ranking, so it stays.
      panelMetaLabel: week.defeated ? finishingBlowLabel : null,
    };
  }

  /**
   * Maps the active week's boss confrontation to its node.
   *
   * @param current - Active week's boss confrontation.
   * @param contributions - The week's damage broken down per player so far.
   * @returns The display-ready node.
   */
  private toCurrentNode(
    current: CurrentBoss,
    contributions: readonly BossContribution[],
  ): BossTimelineNode {
    const remaining = remainingWeekTime(current.weekEnd, this.now());
    const remainingLabel = this.translation.translate('boss.meta.remaining', {
      days: remaining.days,
      hours: remaining.hours,
    });

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
      metaLabel: remainingLabel,
      panelMetaLabel: remainingLabel,
    };
  }

  /**
   * Builds everything a week with a drawn boss shares, whatever its outcome — identity, dates and
   * the hit points readout — leaving each caller to add only its own meta line.
   *
   * @param weekStart - Monday identifying the week, as `YYYY-MM-DD`.
   * @param weekEnd - Sunday identifying the week, as `YYYY-MM-DD`.
   * @param status - The week's outcome/state.
   * @param boss - The boss drawn for that week.
   * @param effectiveHp - The hit points the boss must lose to be defeated this week.
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
    const remainingHp = Math.max(0, effectiveHp - totalDamageDealt);
    const percentage = effectiveHp > 0 ? Math.round((remainingHp / effectiveHp) * 100) : 0;

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
      portraitUrl: boss.imageUrl,
      hasDamage: true,
      hpPercentage: percentage,
      damagePercentage: 100 - percentage,
      hpPercentageLabel: this.translation.translate('boss.hpPercentage', {
        value: percentage,
      }),
      hpLabel: this.translation.translate('boss.hpValue', {
        remaining: formatDamage(remainingHp, language),
        total: formatDamage(effectiveHp, language),
      }),
      barLabel: this.translation.translate(resolveBossHpBarLabelKey(status)),
      metaLabel: null,
      panelMetaLabel: null,
      contributions,
    };
  }

  /**
   * Builds one locked placeholder node for a week ahead with no boss drawn yet.
   *
   * The week itself is known even though its boss isn't — the calendar runs on fixed Monday-to-
   * Sunday periods — so the node still carries its number and dates, and only the opponent stays
   * sealed. They fall back to blank in the one case the anchor is missing: the active week failing
   * to load, which leaves nothing to count forward from.
   *
   * @param index - Zero-based position among the upcoming placeholders.
   * @param currentWeekEnd - The active week's end date as `YYYY-MM-DD`, or `null` when unknown.
   * @returns The display-ready node.
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
      portraitUrl: null,
      hasDamage: false,
      hpPercentage: 0,
      damagePercentage: 0,
      hpPercentageLabel: '',
      hpLabel: '',
      barLabel: '',
      metaLabel: null,
      panelMetaLabel: null,
      contributions: [],
    };
  }
}
