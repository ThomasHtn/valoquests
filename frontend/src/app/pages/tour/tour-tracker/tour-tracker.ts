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
} from '@core/matches/display/match-format.utils';
import {
  resolveResultAccentClass,
  resolveResultTextClass,
} from '@core/matches/display/match-visual.utils';
import { MediaThumbnail } from '@shared/media-thumbnail/media-thumbnail';
import { Tooltip } from '@shared/tooltip/tooltip';
import { TourSampleMatch } from '../tour.model';

/**
 * One evening of match history, built from its pieces since the real one pages and links out.
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

  /**
   * Wins of the evening, for the summary line.
   */
  protected readonly wins = computed(
    () => this.matches().filter((match) => match.result === 'WIN').length,
  );

  /**
   * Losses of the evening, for the summary line.
   */
  protected readonly losses = computed(
    () => this.matches().filter((match) => match.result === 'LOSS').length,
  );

  /**
   * Damage of the whole evening, shown in the header.
   */
  protected readonly totalDamage = computed(() =>
    this.matches().reduce((sum, match) => sum + match.damage, 0),
  );

  /**
   * Accent stripe class of a match result.
   */
  protected readonly resultAccentClass = resolveResultAccentClass;

  /**
   * Text color class of a match result.
   */
  protected readonly resultTextClass = resolveResultTextClass;

  /**
   * Monogram shown when an agent portrait is missing.
   */
  protected readonly agentInitial = resolveAgentInitial;

  /**
   * Map thumbnail URL of a match.
   */
  protected readonly mapImageUrl = resolveMapImageUrl;

  /**
   * Agent portrait URL of a match.
   */
  protected readonly agentImageUrl = resolveAgentImageUrl;

  /**
   * Tooltip key explaining how much of a match's damage counted.
   */
  protected readonly damageHintKey = resolveDamageHintKey;

  /**
   * Translation, to format damage in the current language.
   */
  private readonly translation = inject(Translation);

  /**
   * Formats a damage amount in the current language.
   */
  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }
}
