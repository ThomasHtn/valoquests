import { computed, inject, Service, signal, Signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';

import { BossApi } from '@core/boss/boss-api';
import { bossDamageOf } from '@core/boss/boss-damage.utils';
import { BossTimelineNode, BossContribution } from '@core/boss/boss-timeline.model';
import { resolveScheduledBossCategory } from '@core/boss/boss-visual.utils';
import { BossCategory, BossHistoryWeek, CurrentBoss } from '@core/boss/boss.model';
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
   * The full campaign: one node per week of the run, in run order, each at its own index.
   *
   * The map is exactly as long as a run, whatever has been fought so far, because a run is ten
   * weekly rollovers rather than ten fights — a week can go by with no boss drawn at all, and
   * counting fights would make the map shrink and grow for reasons nobody could read.
   *
   * Each fight is placed at the run week the backend stamps it with, never at its position in the
   * history. Those two only agree while every week of the run had a fight: one week that closed
   * without one — a rollover that never fired, a week nobody's page view reached — used to shift
   * every later week up a slot, so the map wrote each fight's colony reward onto its neighbour's
   * hexagon.
   */
  public readonly nodes: Signal<readonly BossTimelineNode[]> = computed(() => {
    const contributionsByWeekStart = this.contributionsByWeekStart();
    const currentBoss = resourceValue(this.currentResource, null);

    const foughtByRunWeek = new Map<number, BossTimelineNode>();

    for (const week of resourceValue(this.historyResource, null)?.content ?? []) {
      foughtByRunWeek.set(
        week.runWeekIndex,
        this.toHistoryNode(week, contributionsByWeekStart.get(week.weekStart) ?? []),
      );
    }

    if (!currentBoss) {
      // No active week means no calendar anchor and no known run length: show what was fought, in
      // run order, followed by a short tail so the map still reads as a map.
      const fought = [...foughtByRunWeek.entries()]
        .sort(([left], [right]) => left - right)
        .map(([, node]) => node);

      return [
        ...fought,
        ...Array.from({ length: FALLBACK_PLACEHOLDER_COUNT }, (_, index) =>
          this.toUpcomingNode(fought.length + index + 1, null),
        ),
      ];
    }

    foughtByRunWeek.set(
      currentBoss.runWeekIndex,
      this.toCurrentNode(currentBoss, contributionsByWeekStart.get(currentBoss.weekStart) ?? []),
    );

    return Array.from({ length: currentBoss.runWeekCount }, (_, index) => {
      const runWeekIndex = index + 1;
      const fought = foughtByRunWeek.get(runWeekIndex);

      return (
        fought ??
        this.toUpcomingNode(
          runWeekIndex,
          addDays(currentBoss.weekStart, (runWeekIndex - currentBoss.runWeekIndex) * 7),
        )
      );
    });
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
        week.runWeekIndex,
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
        current.runWeekIndex,
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
   * @param runWeekIndex - Position of the week inside its run, from one.
   * @param status - The week's outcome/state.
   * @param boss - The boss drawn for that week.
   * @param effectiveHp - The hit points the boss must lose to be defeated this week.
   * @param totalDamageDealt - Damage the group dealt it.
   * @param contributions - That damage broken down per player.
   * @returns The display-ready node, with a `null` meta line.
   */
  /**
   * The class a week is scheduled to fight, resolved and translated together.
   *
   * Both node builders need the pair and neither should carry the lookup, since a fought week and a
   * sealed one answer it identically: the class comes from the calendar, only the opponent comes
   * from the draw.
   *
   * @param runWeekIndex - Position of the week inside its run, from one.
   * @returns The scheduled class and its label, both `null` outside the run's weeks.
   */
  private scheduledCategoryOf(runWeekIndex: number): {
    scheduledCategory: BossCategory | null;
    scheduledCategoryLabel: string | null;
  } {
    const scheduledCategory = resolveScheduledBossCategory(runWeekIndex);

    return {
      scheduledCategory,
      scheduledCategoryLabel:
        scheduledCategory === null
          ? null
          : this.translation.translate(`boss.category.${scheduledCategory}`),
    };
  }

  private toFoughtNode(
    weekStart: string,
    weekEnd: string,
    runWeekIndex: number,
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
      weekStart,
      status,
      runWeekIndex,
      weekNumber: isoWeekNumber(weekStart),
      // Numbered inside the run, not by the calendar. The timeline of a ten-week campaign used to
      // read "Semaine 35" through "Semaine 44" under a context bar announcing "Semaine 2 sur 10",
      // so the same week carried two numbers on the same screen and neither matched the ladder of
      // difficulty in the rules, which is indexed 1 to 10. `weekNumber` above keeps the ISO figure
      // for anything that genuinely needs the calendar; the dates below say which days these are.
      weekLabel: this.translation.translate('boss.week.label', { number: runWeekIndex }),
      dateRangeLabel: formatDateRange(weekStart, weekEnd),
      statusLabel: this.translation.translate(resolveBossStatusLabelKey(status)),
      bossName: boss.name,
      bossDescription: boss.description,
      categoryLabel: this.translation.translate(`boss.category.${boss.category}`),
      category: boss.category,
      ...this.scheduledCategoryOf(runWeekIndex),
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
   * Builds one locked placeholder node for a week of the run with no boss drawn.
   *
   * The week itself is known even though its boss isn't — the calendar runs on fixed Monday-to-
   * Sunday periods — so the node still carries its number and dates, and only the opponent stays
   * sealed. They fall back to blank in the one case there is no anchor: the active week failing to
   * load, which leaves nothing to count the calendar from.
   *
   * @param runWeekIndex - Position of the week inside its run, from one.
   * @param weekStart - Monday beginning that week as `YYYY-MM-DD`, or `null` when unknown.
   * @returns The display-ready node.
   */
  private toUpcomingNode(runWeekIndex: number, weekStart: string | null): BossTimelineNode {
    return {
      id: `upcoming-${runWeekIndex}`,
      weekStart,
      status: 'upcoming',
      runWeekIndex,
      weekNumber: weekStart === null ? null : isoWeekNumber(weekStart),
      // Numbered inside the run, like the fought nodes above. Unlike them it holds even without a
      // resolved `weekStart`: the run index is the node's own position, known before its calendar
      // week is.
      weekLabel: this.translation.translate('boss.week.label', { number: runWeekIndex }),
      dateRangeLabel: weekStart === null ? null : formatDateRange(weekStart, addDays(weekStart, 6)),
      statusLabel: this.translation.translate(resolveBossStatusLabelKey('upcoming')),
      bossName: this.translation.translate('boss.upcoming.name'),
      bossDescription: this.translation.translate('boss.upcoming.description'),
      categoryLabel: null,
      category: null,
      ...this.scheduledCategoryOf(runWeekIndex),
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
