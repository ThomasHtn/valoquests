import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { LedgerCell, LedgerRow } from '../campaign.model';

/**
 * One cell of the ledger: a week of one resource.
 *
 * The host is the grid item itself, so the row's colour and the bar heights ride on it as classes
 * and custom properties. A week not played yet is decorative; the others describe themselves.
 */
@Component({
  selector: 'app-ledger-cell',
  imports: [TranslatePipe],
  templateUrl: './ledger-cell.html',
  styleUrl: './ledger-cell.css',
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
  public readonly row = input.required<LedgerRow>();
  public readonly cell = input.required<LedgerCell>();
  public readonly label = input.required<string>();

  private readonly translation = inject(Translation);

  protected readonly ahead = computed(() => this.cell().kind === 'ahead');

  protected readonly hostClass = computed(() => {
    const kind = this.cell().kind;
    const state = kind === 'now' ? ' lg-cell--now' : kind === 'ahead' ? ' lg-cell--ahead' : '';
    return `lg-cell lg-cell--${this.row().key}${state}${kind === 'ahead' ? '' : ' focus-ring'}`;
  });

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }
}
