import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LucideDynamicIcon } from '@lucide/angular';

import { FigurePipe } from '@core/i18n/figure-pipe';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Tooltip } from '@shared/tooltip/tooltip';
import { BoardRow } from '../challenges.model';
import { toRuleParts } from '../challenges.utils';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';

/**
 * A challenge as the board's first column opens on it: the hexagon beside the difficulty and the
 * name, the gain per operator in the corner, then the rule with its numbers in bold.
 */
@Component({
  selector: 'app-board-head',
  imports: [LucideDynamicIcon, FigurePipe, TranslatePipe, Tooltip],
  templateUrl: './board-head.html',
  styleUrl: './board-head.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BoardHead {
  /**
   * The one icon of each concept, read by the template's `svg[lucideIcon]`.
   */
  protected readonly concepts = CONCEPT_ICONS;

  public readonly row = input.required<BoardRow>();

  /**
   * The rule cut into plain words and the numbers it holds.
   */
  protected readonly ruleParts = computed(() => toRuleParts(this.row().description));
}
