import { NgOptimizedImage } from '@angular/common';
import { Component, computed, effect, ElementRef, inject } from '@angular/core';
import { LucideLock, LucideSkull } from '@lucide/angular';

import { BossApi } from '@core/boss/boss-api';
import { BossHistoryWeek, CurrentBoss } from '@core/boss/boss.model';
import {
  resolveBossCategoryColorClass,
  resolveBossHpBarColorClass,
} from '@core/boss/boss-visual.utils';
import { formatDateRange, isoWeekNumber } from '@core/date/week-period.utils';
import { anyError, anyLoading, reloadAll, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import { PlayersApi } from '@core/players/players-api';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { ProgressBar } from '@shared/progress-bar/progress-bar';
import { ResourceState } from '@shared/resource-state/resource-state';
import { SKELETON_ROWS } from '@shared/resource-state/skeleton.constants';
import {
  HEX_FRAME_EDGES,
  resolveBossStatusLabelKey,
  resolveBossTimelineTier,
} from './boss-timeline.constants';
import { BossTimelineNode } from './boss.model';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * Number of locked placeholder markers rendered after the current week, representing weeks ahead
 * whose boss doesn't exist yet — the backend only ever draws a week's boss lazily, once that week
 * becomes current (see `DefaultWeeklyBossSelectionService`), so there is no real data to show for
 * them.
 */
const UPCOMING_PLACEHOLDER_COUNT = 3;

/**
 * Boss battle timeline page.
 *
 * A vertical "battle map" from the oldest finalized week to the active one and a handful of locked
 * weeks ahead, auto-scrolled to the current week's marker on load.
 */
@Component({
  selector: 'app-boss',
  imports: [
    TranslatePipe,
    Avatar,
    ChampionBadge,
    NgOptimizedImage,
    ProgressBar,
    ResourceState,
    LucideLock,
    LucideSkull,
  ],
  templateUrl: './boss.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Boss {
  /**
   * Data-access service backing the boss history and current-week resources.
   */
  private readonly bossApi = inject(BossApi);

  /**
   * Data-access service backing the shared players resource, used to resolve the finishing
   * blow's avatar.
   */
  private readonly playersApi = inject(PlayersApi);

  /**
   * Data-access service backing the reigning-champion lookup.
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
  );

  /**
   * Whether any backing resource failed to load.
   */
  protected readonly hasError = anyError(
    this.historyResource,
    this.currentResource,
    this.playersResource,
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
   */
  private readonly championPlayerId = computed(() =>
    resolveChampionPlayerId(resourceValue(this.rankingApi.latestFinalizedWeek, null)),
  );

  /**
   * The full battle timeline: every finalized week oldest-first, the active week, then a fixed
   * number of locked placeholders for weeks ahead.
   */
  protected readonly nodes = computed<readonly BossTimelineNode[]>(() => {
    const avatarUrlByPlayerId = this.avatarUrlByPlayerId();
    const championPlayerId = this.championPlayerId();

    const historyNodes = [...(resourceValue(this.historyResource, null)?.content ?? [])]
      .reverse()
      .map((week) => this.toHistoryNode(week, avatarUrlByPlayerId, championPlayerId));

    const currentBoss = resourceValue(this.currentResource, null);
    const currentNode = currentBoss ? [this.toCurrentNode(currentBoss)] : [];

    const upcomingNodes = Array.from({ length: UPCOMING_PLACEHOLDER_COUNT }, (_, index) =>
      this.toUpcomingNode(index),
    );

    return [...historyNodes, ...currentNode, ...upcomingNodes];
  });

  /**
   * Hit points the active boss has left, floored at zero once damage dealt reaches its effective
   * hit points.
   */
  protected readonly currentRemainingHp = computed(() => {
    const boss = resourceValue(this.currentResource, null);
    return boss ? Math.max(0, boss.effectiveHp - boss.totalDamageDealt) : 0;
  });

  /**
   * Share of hit points the active boss has left, from 0 to 100.
   */
  protected readonly currentRemainingPercentage = computed(() => {
    const boss = resourceValue(this.currentResource, null);
    return boss && boss.effectiveHp > 0
      ? Math.max(0, Math.round((this.currentRemainingHp() / boss.effectiveHp) * 100))
      : 0;
  });

  /**
   * Fill color utility for the active boss's health bar.
   */
  protected readonly currentHpBarColorClass = computed(() =>
    resolveBossHpBarColorClass(this.currentRemainingPercentage()),
  );

  /**
   * Effective hit points of the active boss, read directly from {@link currentResource} rather
   * than the current timeline node's `effectiveHp` — `BossTimelineNode` types it as `number | null`
   * across every status, so this avoids re-narrowing it in the template.
   */
  protected readonly currentEffectiveHp = computed(
    () => resourceValue(this.currentResource, null)?.effectiveHp ?? 0,
  );

  /**
   * Placeholder line widths driving the loading skeleton.
   */
  protected readonly skeletonRows = SKELETON_ROWS;

  /**
   * Six shortened edges tracing the hex marker's segmented frame, shared by every node.
   */
  protected readonly hexFrameEdges = HEX_FRAME_EDGES;

  /**
   * Resolves a timeline node's visual tier (frame color/glow, badge, connector) from its status,
   * exposed to the template.
   */
  protected readonly timelineTier = resolveBossTimelineTier;

  /**
   * Resolves a timeline node's status badge translation key, exposed to the template.
   */
  protected readonly statusLabelKey = resolveBossStatusLabelKey;

  /**
   * Background gradient for the timeline's center connecting line: cleared ground behind it in
   * green, fading to purple at the active week's position, then flat, muted ground ahead — a
   * progress readout for the whole battle map at a glance, without measuring every row's rendered
   * height to color each connector segment individually.
   */
  protected readonly timelineLineGradient = computed(() => {
    const nodes = this.nodes();
    const currentIndex = nodes.findIndex((node) => node.status === 'current');
    const progressPercentage =
      nodes.length > 1 && currentIndex >= 0
        ? Math.round((currentIndex / (nodes.length - 1)) * 100)
        : 0;

    return (
      `linear-gradient(to bottom, var(--color-accent-green) 0%, ` +
      `var(--color-accent-purple) ${progressPercentage}%, ` +
      `var(--color-surface-700) ${progressPercentage}%, var(--color-surface-800) 100%)`
    );
  });

  /**
   * Scrolls the current week's marker into view once, the first time the timeline finishes
   * loading — the page's one-time "you are here" cue on a potentially long battle map.
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
  }

  /**
   * Reloads every backing resource after a failure.
   */
  protected reload(): void {
    this.hasScrolledToCurrentNode = false;
    reloadAll(this.historyResource, this.currentResource, this.playersResource);
  }

  /**
   * Maps one finalized week to its timeline node.
   *
   * @param week - Finalized boss confrontation.
   * @param avatarUrlByPlayerId - Avatar URL per player id.
   * @param championPlayerId - Id of the reigning weekly "Champion", or `null` while unknown.
   * @returns The display-ready timeline node.
   */
  private toHistoryNode(
    week: BossHistoryWeek,
    avatarUrlByPlayerId: ReadonlyMap<number, string | null>,
    championPlayerId: number | null,
  ): BossTimelineNode {
    return {
      id: week.weekStart,
      status: week.defeated ? 'defeated' : 'survived',
      weekLabel: this.translation.translate('boss.week.label', {
        number: isoWeekNumber(week.weekStart),
      }),
      dateRangeLabel: formatDateRange(week.weekStart, week.weekEnd),
      bossName: week.boss.name,
      bossDescription: week.boss.description,
      categoryLabel: this.translation.translate(`boss.category.${week.boss.category}`),
      categoryColorClass: resolveBossCategoryColorClass(week.boss.category),
      portraitUrl: week.boss.imageUrl,
      effectiveHp: week.effectiveHp,
      totalDamageDealt: week.totalDamageDealt,
      defeatedByPlayerDisplayName: week.defeatedByPlayerDisplayName,
      defeatedByAvatarUrl: week.defeatedByPlayerId
        ? (avatarUrlByPlayerId.get(week.defeatedByPlayerId) ?? null)
        : null,
      defeatedByIsChampion:
        week.defeatedByPlayerId !== null && week.defeatedByPlayerId === championPlayerId,
    };
  }

  /**
   * Maps the active week's boss confrontation to its timeline node.
   *
   * @param current - Active week's boss confrontation.
   * @returns The display-ready timeline node.
   */
  private toCurrentNode(current: CurrentBoss): BossTimelineNode {
    return {
      id: current.weekStart,
      status: 'current',
      weekLabel: this.translation.translate('boss.week.label', {
        number: isoWeekNumber(current.weekStart),
      }),
      dateRangeLabel: formatDateRange(current.weekStart, current.weekEnd),
      bossName: current.boss.name,
      bossDescription: current.boss.description,
      categoryLabel: this.translation.translate(`boss.category.${current.boss.category}`),
      categoryColorClass: resolveBossCategoryColorClass(current.boss.category),
      portraitUrl: current.boss.imageUrl,
      effectiveHp: current.effectiveHp,
      totalDamageDealt: current.totalDamageDealt,
      defeatedByPlayerDisplayName: null,
      defeatedByAvatarUrl: null,
      defeatedByIsChampion: false,
    };
  }

  /**
   * Builds one locked placeholder node for a week ahead with no boss drawn yet.
   *
   * @param index - Zero-based position among the upcoming placeholders, used only for its id.
   * @returns The display-ready timeline node.
   */
  private toUpcomingNode(index: number): BossTimelineNode {
    return {
      id: `upcoming-${index}`,
      status: 'upcoming',
      weekLabel: null,
      dateRangeLabel: null,
      bossName: this.translation.translate('boss.upcoming.name'),
      bossDescription: this.translation.translate('boss.upcoming.description'),
      categoryLabel: null,
      categoryColorClass: null,
      portraitUrl: null,
      effectiveHp: null,
      totalDamageDealt: null,
      defeatedByPlayerDisplayName: null,
      defeatedByAvatarUrl: null,
      defeatedByIsChampion: false,
    };
  }
}
