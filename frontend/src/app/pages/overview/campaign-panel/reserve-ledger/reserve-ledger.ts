import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { LucideChevronDown, LucideTrendingUp, LucideDynamicIcon } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Tooltip } from '@shared/tooltip/tooltip';
import { Translation } from '@core/i18n/translation';
import { LedgerCell, LedgerColumn, LedgerRow } from '../campaign-panel.model';
import { LedgerCellView } from './ledger-cell';
import { CONCEPT_ICONS } from '@core/concepts/concept.constants';

/**
 * The reserves, week after week: a column per planet, a row per resource.
 *
 * Above the ground, what the week brought in; below, hatched, what Sunday's rescue spent; the
 * dashed level crossing into the next column is what was carried over — the stocks never start
 * from zero. Folded at rest onto its title bar.
 */
@Component({
  selector: 'app-reserve-ledger',
  imports: [
    LucideDynamicIcon,
    Tooltip,
    TranslatePipe,
    LedgerCellView,
    LucideChevronDown,
    LucideTrendingUp,
  ],
  templateUrl: './reserve-ledger.html',
  styleUrl: './reserve-ledger.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReserveLedger {
  /**
   * The one icon of each concept, read by the template's `svg[lucideIcon]`.
   */
  protected readonly concepts = CONCEPT_ICONS;

  public readonly columns = input.required<readonly LedgerColumn[]>();

  public readonly rows = input.required<readonly LedgerRow[]>();

  private readonly translation = inject(Translation);

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  /**
   * What the cell says to a reader who cannot see the bars.
   */
  protected cellLabel(row: LedgerRow, cell: LedgerCell): string {
    const unit = this.translation.translate(`common.resource.${row.key}`).toLowerCase();
    const key = cell.kind === 'now' ? 'campaign.ledger.cellNow' : 'campaign.ledger.cellSettled';
    return this.translation.translate(key, {
      index: cell.index,
      planet: cell.planetName,
      unit,
      got: this.format(cell.got),
      spent: this.format(cell.spent),
      carry: this.format(cell.carry),
    });
  }
}
