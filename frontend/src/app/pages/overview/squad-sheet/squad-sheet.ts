import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LucideDynamicIcon } from '@lucide/angular';

import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import { formatFigure } from '@core/i18n/format/number-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { StreakGauge } from '@shared/streak-gauge/streak-gauge';
import { TitleBadge } from '@shared/title-badge/title-badge';
import { Tooltip } from '@shared/tooltip/tooltip';

import { SQUAD_COLUMNS } from './squad-sheet.constants';
import { SquadRow } from './squad-sheet.model';

/**
 * Day's tally per operator; the resource columns add up to the tally above.
 */
@Component({
  selector: 'app-squad-sheet',
  imports: [
    LucideDynamicIcon,
    NgTemplateOutlet,
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
   * Header columns, named while the sheet lays its rows out as a table.
   */
  protected readonly columns = SQUAD_COLUMNS;

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
    return formatFigure(amount, this.translation.language());
  }
}
