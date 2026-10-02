import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LucideDynamicIcon } from '@lucide/angular';

import { FigurePipe } from '@core/i18n/figure-pipe';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Tooltip } from '@shared/tooltip/tooltip';
import { BoardRow } from '../challenges.model';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';

/**
 * What a challenge is, as the board's first column and a phone card both open on it: the hexagon,
 * the key line (a closed day said on a daily), the name, the gain per operator, the rule.
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
   * The one icon of each concept, read by the template's `svg[lucideIcon]`.
   */
  protected readonly concepts = CONCEPT_ICONS;

  public readonly row = input.required<BoardRow>();
}
