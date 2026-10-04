import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { LedgerCell, LedgerRow } from '../../campaign-panel.model';

/**
 * Ledger cell; the host is the grid item and carries the bar heights as custom properties.
 */
@Component({
  selector: 'app-ledger-cell',
  imports: [TranslatePipe],
  templateUrl: './ledger-cell.html',
  styleUrl: './ledger-cell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class]': 'hostClass()',
    '[attr.aria-hidden]': 'ahead() ? true : null',
    '[attr.aria-label]': 'ahead() ? null : label()',
    '[attr.role]': 'ahead() ? null : "img"',
    '[attr.tabindex]': 'ahead() ? null : 0',
    '[style.--got]': 'cell().gotShare',
    '[style.--spent]': 'cell().spentShare',
    '[style.--carry]': 'cell().carryShare',
  },
})
export class LedgerCellView {
  /**
   * Resource row the cell belongs to, which picks its colour.
   */
  public readonly row = input.required<LedgerRow>();

  /**
   * Week figures the bars are scaled from.
   */
  public readonly cell = input.required<LedgerCell>();

  /**
   * Accessible description of the week, read instead of the bars.
   */
  public readonly label = input.required<string>();

  /**
   * Active language, to format the bar values.
   */
  private readonly translation = inject(Translation);

  /**
   * Whether the week is still to come, drawn empty and hidden from assistive tech.
   */
  protected readonly ahead = computed(() => this.cell().kind === 'ahead');

  /**
   * Host classes for the resource, the week state and focusability.
   */
  protected readonly hostClass = computed(() => {
    const kind = this.cell().kind;
    const state = kind === 'now' ? ' lg-cell--now' : kind === 'ahead' ? ' lg-cell--ahead' : '';
    return `lg-cell lg-cell--${this.row().key}${state}${kind === 'ahead' ? '' : ' focus-ring'}`;
  });

  /**
   * Formats an amount in the active language for the bar values.
   */
  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }
}
