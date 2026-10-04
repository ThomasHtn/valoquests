import { Component, computed, inject, input } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { LucideChevronLeft } from '@lucide/angular';

import { primaryTitle } from '@core/campaign/titles/campaign-title.utils';
import { WeeklyTitle } from '@core/campaign/titles/campaign-title.model';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { formatLocalDayMonth, formatLocalTime } from '@core/date/date-format.utils';
import { isNotFound, resourceValue } from '@core/http/resource-state.utils';
import { parseRouteId } from '@core/navigation/navigation-route-id.utils';
import {
  resolveAgentImageUrl,
  resolveAgentInitial,
  resolveDamageHintKey,
  resolveMapImageUrl,
  resolveMatchScore,
} from '@core/matches/display/match-format.utils';
import { resolveResultTextClass } from '@core/matches/display/match-visual.utils';
import { MatchTeammate } from '@core/matches/match.model';
import { MatchesApi } from '@core/matches/matches-api';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolveCompetitiveTierVisual } from '@core/players/competitive-tier/player-competitive-tier.utils';
import { resolvePlayerAvatarUrl } from '@core/players/avatar/player-avatar.utils';
import {
  formatHeadshotPercentage,
  formatKda,
  formatScore,
} from '@core/players/player-format.utils';
import { resolveKdVisual } from '@core/players/stats/player-stats.utils';
import { RankingApi } from '@core/ranking/ranking-api';
import { resolveChampionPlayerId } from '@core/ranking/ranking-champion.utils';
import { PageHeader } from '@layout/page-header/page-header';
import { PAGE_LAYOUT_CLASS } from '@layout/page-layout.constants';
import { Avatar } from '@shared/avatar/avatar';
import { Button } from '@shared/button/button';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { ResourceState } from '@shared/resource-state/resource-state';
import { TitleBadge } from '@shared/title-badge/title-badge';
import { Tooltip } from '@shared/tooltip/tooltip';
import { MediaThumbnail } from '@shared/media-thumbnail/media-thumbnail';
import { buildNotFoundPlate } from '../player-profile.utils';

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
  protected readonly playerId = computed(() => parseRouteId(this.id()));

  /**
   * Parsed player-match id, `null` when malformed.
   */
  protected readonly playerMatchId = computed(() => parseRouteId(this.matchId()));

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
   * Local map image.
   */
  protected readonly mapImageUrl = resolveMapImageUrl;

  /**
   * Local agent portrait.
   */
  protected readonly agentImageUrl = resolveAgentImageUrl;

  /**
   * Monogram standing in for an agent portrait.
   */
  protected readonly agentInitial = resolveAgentInitial;

  /**
   * Id of the reigning weekly champion, `null` while unknown.
   */
  protected readonly championPlayerId = computed(() =>
    resolveChampionPlayerId(resourceValue(this.rankingApi.latestFinalizedWeek, null)),
  );

  /**
   * Current week's title held by each player id.
   */
  protected readonly titlesByPlayer = computed(() => {
    const byPlayer = new Map<number, WeeklyTitle>();
    for (const entry of resourceValue(this.rankingApi.current, null)?.ranking ?? []) {
      const title = primaryTitle(entry.titles);
      if (title !== null) {
        byPlayer.set(entry.player.id, title);
      }
    }
    return byPlayer;
  });

  /**
   * Bundled avatar of a teammate.
   */
  protected readonly avatarUrl = resolvePlayerAvatarUrl;

  /**
   * Round score of the match.
   */
  protected readonly matchScore = resolveMatchScore;

  /**
   * Text colour of the match result.
   */
  protected readonly resultTextClass = resolveResultTextClass;

  /**
   * Text colour of a KDA.
   */
  protected readonly kdVisual = resolveKdVisual;

  /**
   * Formats a combat or damage score.
   */
  protected readonly formatScore = formatScore;

  /**
   * Start day of the match.
   */
  protected readonly matchDay = computed(() => {
    const match = this.match();
    return match ? formatLocalDayMonth(match.startedAt, this.translation.language()) : '';
  });

  /**
   * Formats the start time.
   */
  protected readonly matchTime = formatLocalTime;

  /**
   * Duration as `"32 min"`, `''` when Henrik reported none.
   */
  protected readonly durationLabel = computed(() => {
    const seconds = this.match()?.durationSeconds;
    return seconds
      ? this.translation.translate('playerProfile.matches.detail.duration', {
          minutes: Math.round(seconds / 60),
        })
      : '';
  });

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
   * Formats a KDA ratio.
   */
  protected readonly formatKda = (kda: number | null): string =>
    formatKda(kda, this.translation.language());

  /**
   * Formats a headshot percentage.
   */
  protected readonly formatHeadshotPercentage = (percentage: number | null): string =>
    formatHeadshotPercentage(percentage, this.translation.language());

  /**
   * Damage amount grouped in the active language, e.g. `"1 250"`.
   */
  protected formatDamageAmount(damage: number): string {
    return formatDamage(damage, this.translation.language());
  }

  /**
   * Explains the damage coefficient the day's ladder applied, worded like the history row.
   */
  protected damageExplanation(coefficientPercent: number): string {
    return this.translation.translate(resolveDamageHintKey(coefficientPercent), {
      percent: coefficientPercent,
    });
  }

  /**
   * Result colour of a teammate.
   */
  protected teammateResultClass(teammate: MatchTeammate): string {
    return resolveResultTextClass(teammate.result);
  }
}
