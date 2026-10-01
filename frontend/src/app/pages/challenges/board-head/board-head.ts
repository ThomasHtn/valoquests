import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LucideUsers, LucideZap } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Tooltip } from '@shared/tooltip/tooltip';
import { BoardRow } from '../challenges.model';
import { toRuleParts } from '../challenges.utils';

/**
 * A challenge as the board's first column opens on it: the hexagon beside the difficulty and the
 * name, the gain per operator in the corner, then the rule with its numbers in bold.
 */
@Component({
  selector: 'app-board-head',
  imports: [TranslatePipe, Tooltip, LucideUsers, LucideZap],
  templateUrl: './board-head.html',
  styleUrl: './board-head.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BoardHead {
  public readonly row = input.required<BoardRow>();

  /**
   * The rule cut into plain words and the numbers it holds.
   */
  protected readonly ruleParts = computed(() => toRuleParts(this.row().description));
}
