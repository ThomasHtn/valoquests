import { Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { formatLocalDayMonth, formatLocalTime } from '@core/date/date-time.utils';
import { resourceValue } from '@core/http/resource-state.utils';
import {
  resolveAgentImageUrl,
  resolveAgentInitial,
  resolveMapImageUrl,
  resolveMatchScore,
} from '@core/matches/match-format.utils';
import { resolveResultTextClass } from '@core/matches/match-visual.utils';
import { MatchTeammate } from '@core/matches/match.model';
import { MatchesApi } from '@core/matches/matches-api';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resolveCompetitiveTierVisual } from '@core/players/competitive-tier.utils';
import { resolvePlayerAvatarUrl } from '@core/players/player-avatar.utils';
import {
  formatHeadshotPercentage,
  formatKda,
  formatScore,
} from '@core/players/player-format.utils';
import { resolveKdaVisual } from '@core/players/player-stats.utils';
import { PageHeader } from '@layout/page-header/page-header';
import { PAGE_LAYOUT_CLASS } from '@pages/page-layout.constants';
import { Avatar } from '@shared/avatar/avatar';
import { ResourceState } from '@shared/resource-state/resource-state';
import { Tooltip } from '@shared/tooltip/tooltip';
import { MediaThumbnail } from '../media-thumbnail/media-thumbnail';

/**
 * Full detail of one of a tracked player's matches, reached from a row of their match history.
 *
 * A superset of the history row it was opened from: the same figures, plus the shot-type breakdown
 * behind the headshot rate, the raw damage and round count, and every other tracked player found in
 * the same match — the squad is small enough that two of them routinely land in the same lobby.
 */
@Component({
  selector: 'app-match-detail',
  imports: [TranslatePipe, Avatar, MediaThumbnail, PageHeader, ResourceState, RouterLink, Tooltip],
  templateUrl: './match-detail.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class MatchDetail {
  /**
   * Data-access service backing the match detail resource.
   */
  private readonly matchesApi = inject(MatchesApi);

  /**
   * i18n service, used to resolve the tier badge and the damage tooltip.
   */
  private readonly translation = inject(Translation);

  /**
   * Route parameter naming the player, bound by `withComponentInputBinding`.
   */
  public readonly id = input.required<string>();

  /**
   * Route parameter naming the match, bound by `withComponentInputBinding`.
   */
  public readonly matchId = input.required<string>();

  /**
   * The route's player identifier, parsed once.
   */
  protected readonly playerId = computed(() => Number(this.id()));

  /**
   * The route's player-match identifier, parsed once.
   */
  protected readonly playerMatchId = computed(() => Number(this.matchId()));

  /**
   * Reactive resource fetching the requested match's full detail.
   */
  protected readonly detailResource = this.matchesApi.detail(this.playerId, this.playerMatchId);

  /**
   * The requested match's full detail, or `null` while loading or on error.
   */
  protected readonly match = computed(() => resourceValue(this.detailResource, null));

  /**
   * Route back to the player's own profile.
   */
  protected readonly backLink = computed(() => `/players/${this.playerId()}`);

  /**
   * Resolves the local map image matching the match, exposed to the template.
   */
  protected readonly mapImageUrl = resolveMapImageUrl;

  /**
   * Resolves the local portrait matching an agent, exposed to the template.
   */
  protected readonly agentImageUrl = resolveAgentImageUrl;

  /**
   * Resolves the monogram standing in for an agent's portrait, exposed to the template.
   */
  protected readonly agentInitial = resolveAgentInitial;

  /**
   * Resolves a player's bundled avatar, exposed to the template for the teammate rows.
   */
  protected readonly avatarUrl = resolvePlayerAvatarUrl;

  /**
   * Resolves the match's round score, exposed to the template.
   */
  protected readonly matchScore = resolveMatchScore;

  /**
   * Resolves the text colour carrying the match's result, exposed to the template.
   */
  protected readonly resultTextClass = resolveResultTextClass;

  /**
   * Resolves a KDA's text colour, exposed to the template.
   */
  protected readonly kdaVisual = resolveKdaVisual;

  /**
   * Formats a KDA ratio, exposed to the template.
   */
  protected readonly formatKda = formatKda;

  /**
   * Formats a headshot percentage, exposed to the template.
   */
  protected readonly formatHeadshotPercentage = formatHeadshotPercentage;

  /**
   * Formats a combat or damage score, exposed to the template.
   */
  protected readonly formatScore = formatScore;

  /**
   * Formats the match's start day, exposed to the template.
   */
  protected readonly matchDay = computed(() => {
    const match = this.match();
    return match ? formatLocalDayMonth(match.startedAt, this.translation.language()) : '';
  });

  /**
   * Formats the match's start time, exposed to the template.
   */
  protected readonly matchTime = formatLocalTime;

  /**
   * Formats the match's duration as `"32 min"`, or `''` when Henrik never reported one.
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
   * Resolves the coloured label for the match's competitive tier.
   */
  protected readonly tierLabel = computed(() => {
    const tier = this.match()?.competitiveTier;
    return tier
      ? resolveCompetitiveTierVisual(tier, (key) => this.translation.translate(key))
      : null;
  });

  /**
   * Formats a ValoQuests damage amount, grouped in the active language, exposed to the template.
   *
   * @param damage - Damage the match was worth.
   * @returns The grouped amount, e.g. `"1 250"`.
   */
  protected formatDamageAmount(damage: number): string {
    return formatDamage(damage, this.translation.language());
  }

  /**
   * Explains the amount the match was worth: which coefficient the day's ladder applied to it, or
   * why it was worth nothing at all. Same wording as the history row it was opened from.
   *
   * @param coefficientPercent - Share of its base damage the match kept.
   * @returns The explanatory sentence.
   */
  protected damageExplanation(coefficientPercent: number): string {
    return this.translation.translate(
      coefficientPercent === 0
        ? 'playerProfile.matches.damage.unvalued'
        : 'playerProfile.matches.damage.coefficient',
      { percent: coefficientPercent },
    );
  }

  /**
   * Resolves a teammate's own result colour, mirroring {@link resultTextClass}.
   *
   * @param teammate - The teammate row to colour.
   * @returns The Tailwind text-colour utility to apply.
   */
  protected teammateResultClass(teammate: MatchTeammate): string {
    return resolveResultTextClass(teammate.result);
  }
}
