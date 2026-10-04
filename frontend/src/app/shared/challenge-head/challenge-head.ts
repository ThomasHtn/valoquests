import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LucideDynamicIcon } from '@lucide/angular';

import { FigurePipe } from '@core/i18n/format/figure-pipe';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Tooltip } from '@shared/tooltip/tooltip';
import { BoardRow } from '@core/challenges/card/challenge-card.model';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';

/**
 * Challenge head shared by the board's first column and phone cards.
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
   * Icon of each concept.
   */
  protected readonly concepts = CONCEPT_ICONS;

  /**
   * The challenge the head describes.
   */
  public readonly row = input.required<BoardRow>();
}
