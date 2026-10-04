import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideDynamicIcon } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Avatar } from '@shared/avatar/avatar';
import { Tooltip } from '@shared/tooltip/tooltip';
import { SquadRow } from './squad-sheet.model';
import { TitleBadge } from '@shared/title-badge/title-badge';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { StreakGauge } from '@shared/streak-gauge/streak-gauge';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';

/**
 * Day's tally per operator; the resource columns add up to the tally above.
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
   * Icon of each concept, for the template's `svg[lucideIcon]`.
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

  /**
   * Active language, to format the operators' figures.
   */
  private readonly translation = inject(Translation);

  /**
   * Formats an amount in the active language for the resource columns.
   */
  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }
}
