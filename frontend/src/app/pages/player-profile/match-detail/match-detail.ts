import { Component, computed, inject, input } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { LucideChevronLeft } from '@lucide/angular';

import { buildTitlesByPlayer } from '@core/campaign/titles/campaign-title.utils';
import { formatCampaignDayMonth, formatCampaignTime } from '@core/date/date-format.utils';
import { isNotFound, resourceValue } from '@core/http/resource-state.utils';
import { formatFigure } from '@core/i18n/format/number-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import {
  resolveAgentImageUrl,
  resolveAgentInitial,
  resolveDamageHintKey,
  resolveMapImageUrl,
  resolveMatchScore,
} from '@core/matches/display/match-format.utils';
import { resolveResultTone } from '@core/matches/display/match-visual.utils';
import { MatchesApi } from '@core/matches/matches-api';
import { parseRouteId } from '@core/navigation/navigation-route-id.utils';
import { resolveCompetitiveTierVisual } from '@core/players/competitive-tier/player-competitive-tier.utils';
import { formatHeadshotPercentage } from '@core/players/player-format.utils';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { PageHeader } from '@layout/page-header/page-header';
import { PAGE_LAYOUT_CLASS } from '@layout/page-layout.constants';
import { Avatar } from '@shared/avatar/avatar';
import { Button } from '@shared/button/button';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { MediaThumbnail } from '@shared/media-thumbnail/media-thumbnail';
import { ResourceState } from '@shared/resource-state/resource-state';
import { TitleBadge } from '@shared/title-badge/title-badge';
import { Tooltip } from '@shared/tooltip/tooltip';

import { buildNotFoundPlate } from '../player-profile.utils';
import {
  buildMatchFigures,
  buildMatchShotCounts,
  buildMatchTeammateRows,
} from './match-detail.utils';

/**
 * Match detail: the history row's figures plus shots, raw damage and tracked teammates.
 */
@Component({
  selector: 'app-match-detail',
  imports: [
    TranslatePipe,
    Avatar,
    Button,
    ChampionBadge,
    LucideChevronLeft,
    MediaThumbnail,
    PageHeader,
    ResourceState,
    RouterLink,
    TitleBadge,
    Tooltip,
  ],
  templateUrl: './match-detail.html',
  styleUrl: './match-detail.scss',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class MatchDetail {
  /**
   * Match detail data access.
   */
  private readonly matchesApi = inject(MatchesApi);

  /**
   * Translates the tier badge and the damage tooltip.
   */
  private readonly translation = inject(Translation);

  /**
   * Ranking data access, for the teammates' champion and titles.
   */
  private readonly rankingApi = inject(RankingApi);

  /**
   * Player id route parameter.
   */
  public readonly id = input.required<string>();

  /**
   * Match id route parameter.
   */
  public readonly matchId = input.required<string>();

  /**
   * Gives the not-found plate its address.
   */
  private readonly router = inject(Router);

  /**
   * Parsed player id, `null` when malformed.
   */
  private readonly playerId = computed(() => parseRouteId(this.id()));

  /**
   * Parsed player-match id, `null` when malformed.
   */
  private readonly playerMatchId = computed(() => parseRouteId(this.matchId()));

  /**
   * Full detail of the requested match.
   */
  protected readonly detailResource = this.matchesApi.detail(this.playerId, this.playerMatchId);

  /**
   * Match detail, `null` while loading or on error.
   */
  protected readonly match = computed(() => resourceValue(this.detailResource, null));

  /**
   * Whether the route names no known match: malformed id or backend 404.
   */
  protected readonly notFound = computed(
    () =>
      this.playerId() === null || this.playerMatchId() === null || isNotFound(this.detailResource),
  );

  /**
   * Plate shown instead of the match when not found.
   */
  protected readonly notFoundPlate = computed(() =>
    buildNotFoundPlate(
      (key) => this.translation.translate(key),
      'playerProfile.matches.detail.notFound',
      this.router.url,
    ),
  );

  /**
   * Back to the player's profile, or to the squad when the player id is malformed.
   */
  protected readonly backLink = computed(() => {
    const playerId = this.playerId();
    return playerId === null ? '/players' : `/players/${playerId}`;
  });

  /**
   * Local map image, `null` while loading or for an unknown map.
   */
  protected readonly mapImageUrl = computed(() => {
    const match = this.match();
    return match ? resolveMapImageUrl(match.mapName) : null;
  });

  /**
   * Local agent portrait, `null` while loading or for an unknown agent.
   */
  protected readonly agentImageUrl = computed(() => {
    const match = this.match();
    return match ? resolveAgentImageUrl(match.agentName) : null;
  });

  /**
   * Monogram standing in for a missing map or agent image.
   */
  protected readonly agentInitial = computed(() => {
    const match = this.match();
    return match ? resolveAgentInitial(match.agentName) : '';
  });

  /**
   * Tone of the match result, which the hero's edge takes.
   */
  protected readonly resultTone = computed(() =>
    resolveResultTone(this.match()?.result ?? 'UNKNOWN'),
  );

  /**
   * Round score, `null` for a mode without rounds.
   */
  protected readonly score = computed(() => {
    const match = this.match();
    return match ? resolveMatchScore(match.allyScore, match.enemyScore) : null;
  });

  /**
   * Day, time, mode and duration under the map name.
   */
  protected readonly metaLine = computed(() => {
    const match = this.match();
    if (!match) {
      return '';
    }
    const language = this.translation.language();
    return [
      formatCampaignDayMonth(match.startedAt, language),
      formatCampaignTime(match.startedAt),
      this.translation.translate(`playerProfile.matches.gameMode.${match.gameMode}`),
      // Henrik reports no duration for some matches.
      match.durationSeconds
        ? this.translation.translate('playerProfile.matches.detail.duration', {
            minutes: Math.round(match.durationSeconds / 60),
          })
        : '',
    ]
      .filter((part) => part !== '')
      .join(' · ');
  });

  /**
   * Stat grid of the match.
   */
  protected readonly figures = computed(() => {
    const match = this.match();
    return match ? buildMatchFigures(match, this.translation.language()) : [];
  });

  /**
   * Hits per body zone.
   */
  protected readonly shotCounts = computed(() => {
    const match = this.match();
    return match ? buildMatchShotCounts(match) : [];
  });

  /**
   * Formatted headshot rate.
   */
  protected readonly headshotPercentage = computed(() =>
    formatHeadshotPercentage(this.match()?.headshotPercentage ?? null, this.translation.language()),
  );

  /**
   * Coloured label of the match's competitive tier.
   */
  protected readonly tierLabel = computed(() => {
    const tier = this.match()?.competitiveTier;
    return tier
      ? resolveCompetitiveTierVisual(tier, (key) => this.translation.translate(key))
      : null;
  });

  /**
   * Explains the damage coefficient the day's ladder applied, worded like the history row.
   */
  protected readonly damageExplanation = computed(() => {
    const coefficientPercent = this.match()?.damageCoefficientPercent ?? 100;
    return this.translation.translate(resolveDamageHintKey(coefficientPercent), {
      percent: coefficientPercent,
    });
  });

  /**
   * Current week's title held by each player id.
   */
  private readonly titlesByPlayer = computed(() =>
    buildTitlesByPlayer(resourceValue(this.rankingApi.current, null)?.ranking ?? []),
  );

  /**
   * Other tracked players of the lobby.
   */
  protected readonly teammates = computed(() => {
    const match = this.match();
    if (!match) {
      return [];
    }
    const championPlayerId = resolveChampionPlayerId(
      resourceValue(this.rankingApi.latestFinalizedWeek, null),
    );
    return buildMatchTeammateRows(match, championPlayerId, this.titlesByPlayer());
  });

  /**
   * Damage amount grouped in the active language, e.g. `"1 250"`.
   */
  protected formatDamageAmount(damage: number): string {
    return formatFigure(damage, this.translation.language());
  }
}
