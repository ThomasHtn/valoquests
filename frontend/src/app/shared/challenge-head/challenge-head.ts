import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { LucideDynamicIcon } from '@lucide/angular';

import { BoardRow } from '@core/challenges/card/challenge-card.model';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import { FigurePipe } from '@core/i18n/format/figure-pipe';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Tooltip } from '@shared/tooltip/tooltip';

import { ChallengeHeadGain } from './challenge-head.model';

/**
 * Challenge head of the phone and tour cards: hexagon, title, gain, then the rule.
 */
@Component({
  selector: 'app-challenge-head',
  imports: [LucideDynamicIcon, FigurePipe, TranslatePipe, Tooltip],
  templateUrl: './challenge-head.html',
  styleUrl: './challenge-head.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChallengeHead {
  /**
   * The challenge the head describes.
   */
  public readonly row = input.required<BoardRow>();

  /**
   * Icon of each concept.
   */
  protected readonly concepts = CONCEPT_ICONS;

  /**
   * Gain labelled as wounded while a rescue runs, as ranking points otherwise; the count is the same.
   */
  protected readonly gain = computed<ChallengeHeadGain>(() => {
    const row = this.row();

    return row.rescueActive
      ? {
          icon: CONCEPT_ICONS.wounded,
          tooltipKey: 'challenges.card.woundedTooltip',
          count: row.survivors,
        }
      : {
          icon: CONCEPT_ICONS.points,
          tooltipKey: 'challenges.card.pointsTooltip',
          count: row.survivors,
        };
  });
}
