import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { LucideChevronDown, LucideInfo } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { ColumnLegendEntry } from './column-legend.model';

/**
 * The column explanations a wide table carries in its header tooltips, folded into one tap for the
 * screens where that header is hidden and the cards each keep a bare caption. Closed by default:
 * it is there for the reader who wonders, not in the way of the one who knows.
 */
@Component({
  selector: 'app-column-legend',
  imports: [TranslatePipe, LucideChevronDown, LucideInfo],
  templateUrl: './column-legend.html',
  styleUrl: './column-legend.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
})
export class ColumnLegend {
  /**
   * The columns to explain, in the order the cards show them.
   */
  public readonly entries = input.required<readonly ColumnLegendEntry[]>();
}
