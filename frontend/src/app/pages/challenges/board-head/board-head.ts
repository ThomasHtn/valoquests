import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { LucideDynamicIcon } from '@lucide/angular';

import { BoardRow } from '@core/challenges/card/challenge-card.model';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import { FigurePipe } from '@core/i18n/format/figure-pipe';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Tooltip } from '@shared/tooltip/tooltip';

import { toRuleParts } from '../challenges.utils';

/**
 * The board's challenge cell: hexagon, difficulty and name, gain, then the rule.
 */
@Component({
  selector: 'app-board-head',
  imports: [LucideDynamicIcon, FigurePipe, TranslatePipe, Tooltip],
  templateUrl: './board-head.html',
  styleUrl: './board-head.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class.board-head--weekly]': '!row().daily' },
})
export class BoardHead {
  /**
   * The one icon of each concept.
   */
  protected readonly concepts = CONCEPT_ICONS;

  /**
   * The challenge this cell describes.
   */
  public readonly row = input.required<BoardRow>();

  /**
   * The rule cut into words and numbers.
   */
  protected readonly ruleParts = computed(() => toRuleParts(this.row().description));
}
