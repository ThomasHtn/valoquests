import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideDynamicIcon } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Avatar } from '@shared/avatar/avatar';
import { Tooltip } from '@shared/tooltip/tooltip';
import { SquadRow } from '../overview.model';
import { TitleBadge } from '@shared/title-badge/title-badge';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { StreakGauge } from '@shared/streak-gauge/streak-gauge';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';

/**
 * The squad, by the day: the operator-by-operator detail of the day's tally.
 *
 * The four cells are the day's — what they took from the guardian, the components, the food, the
 * streak — and the three resource columns add up exactly to the tally above. The streak closes
 * the row: it reads as the week's attendance, apart from the day's figures.
 */
@Component({
  selector: 'app-squad-sheet',
  imports: [
    LucideDynamicIcon,
    TranslatePipe,
    RouterLink,
    Avatar,
    Tooltip,
    TitleBadge,
    ChampionBadge,
    StreakGauge,
  ],
  templateUrl: './squad-sheet.html',
  styleUrl: './squad-sheet.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SquadSheet {
  /**
   * The one icon of each concept, read by the template's `svg[lucideIcon]`.
   */
  protected readonly concepts = CONCEPT_ICONS;

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
