import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';

import { LucideChevronDown, LucideDynamicIcon, LucideTrendingUp } from '@lucide/angular';

import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import { formatFigure } from '@core/i18n/format/number-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Tooltip } from '@shared/tooltip/tooltip';

import { LedgerCell, LedgerColumn, LedgerRow } from '../campaign-panel.model';
import { LedgerCellView } from './ledger-cell/ledger-cell';

/**
 * Reserves per week: gained above the ground, spent below, carry-over dashed.
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
   * Concept icons for the template.
   */
  protected readonly concepts = CONCEPT_ICONS;

  /**
   * Campaign weeks heading the ledger, one per planet.
   */
  public readonly columns = input.required<readonly LedgerColumn[]>();

  /**
   * One row per resource, with its weekly cells and totals.
   */
  public readonly rows = input.required<readonly LedgerRow[]>();

  /**
   * Translation service, to format figures and word the cell labels.
   */
  private readonly translation = inject(Translation);

  /**
   * Formats an amount in the active language for the totals and tooltips.
   */
  protected format(amount: number): string {
    return formatFigure(amount, this.translation.language());
  }

  /**
   * Accessible label of a cell.
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
