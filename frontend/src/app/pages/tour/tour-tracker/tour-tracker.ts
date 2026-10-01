import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { LucideArrowDown } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import {
  resolveAgentImageUrl,
  resolveAgentInitial,
  resolveDamageHintKey,
  resolveMapImageUrl,
} from '@core/matches/match-format.utils';
import { resolveResultAccentClass, resolveResultTextClass } from '@core/matches/match-visual.utils';
import { MediaThumbnail } from '@pages/player-profile/media-thumbnail/media-thumbnail';
import { Tooltip } from '@shared/tooltip/tooltip';
import { TourSampleMatch } from '../tour.model';

/**
 * One evening of the profile's match history, cut down to what the first step turns on: every
 * match lands there on its own, and each one carries the damage it dealt to the week's guardian.
 *
 * Built from the history's own pieces (day header, rows, result edge, thumbnails) rather
 * than the component itself, which pages, links out and switches layout at `lg`.
 */
@Component({
  selector: 'app-tour-tracker',
  imports: [TranslatePipe, MediaThumbnail, Tooltip, LucideArrowDown],
  templateUrl: './tour-tracker.html',
  styleUrl: './tour-tracker.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TourTracker {
  /**
   * The evening's matches, newest first.
   */
  public readonly matches = input.required<readonly TourSampleMatch[]>();

  protected readonly wins = computed(
    () => this.matches().filter((match) => match.result === 'WIN').length,
  );

  protected readonly losses = computed(
    () => this.matches().filter((match) => match.result === 'LOSS').length,
  );

  protected readonly totalDamage = computed(() =>
    this.matches().reduce((sum, match) => sum + match.damage, 0),
  );

  protected readonly resultAccentClass = resolveResultAccentClass;

  protected readonly resultTextClass = resolveResultTextClass;

  protected readonly agentInitial = resolveAgentInitial;

  protected readonly mapImageUrl = resolveMapImageUrl;

  protected readonly agentImageUrl = resolveAgentImageUrl;

  protected readonly damageHintKey = resolveDamageHintKey;

  private readonly translation = inject(Translation);

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }
}
