import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideFlame, LucideTarget, LucideWheat, LucideWrench } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Avatar } from '@shared/avatar/avatar';
import { SectionRule } from '@shared/section-rule/section-rule';
import { SquadRow } from '../overview.model';

/**
 * The squad, by the day: the operator-by-operator detail of the day's tally.
 *
 * The four cells are the day's — the streak, what they took from the guardian, the components,
 * the food — and the three resource columns add up exactly to the tally above. The streak opens
 * the row because it is a multiplier: it explains the three figures that follow it.
 */
@Component({
  selector: 'app-squad-sheet',
  imports: [
    TranslatePipe,
    SectionRule,
    RouterLink,
    Avatar,
    LucideFlame,
    LucideTarget,
    LucideWheat,
    LucideWrench,
  ],
  templateUrl: './squad-sheet.html',
  styleUrl: './squad-sheet.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SquadSheet {
  /**
   * One row per operator of the roster, most productive first.
   */
  public readonly rows = input.required<readonly SquadRow[]>();

  /**
   * Operators competing today, shown in the header's hex counter.
   */
  public readonly rosterCount = input.required<number>();

  private readonly translation = inject(Translation);

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }
}
