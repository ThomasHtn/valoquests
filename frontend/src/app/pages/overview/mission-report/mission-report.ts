import {
  ChangeDetectionStrategy,
  Component,
  effect,
  ElementRef,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';
import { LucideCheck, LucideX } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Avatar } from '@shared/avatar/avatar';
import { MissionReport as MissionReportView } from '../overview.model';

/**
 * The Monday report: what Sunday settled, told in the order Sunday settles it, in a dialog over
 * the overview.
 *
 * Opens on its own the first time a settled week is seen, and again from the context bar's
 * button. One figure and a few words per reading; the ranking stays six tiles, never a table.
 */
@Component({
  selector: 'app-mission-report',
  imports: [TranslatePipe, Avatar, LucideCheck, LucideX],
  templateUrl: './mission-report.html',
  styleUrl: './mission-report.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'contents',
    '(document:keydown.escape)': 'onEscape()',
  },
})
export class MissionReport {
  public readonly report = input.required<MissionReportView>();

  public readonly open = input.required<boolean>();

  public readonly closed = output<void>();

  private readonly translation = inject(Translation);

  private readonly panel = viewChild<ElementRef<HTMLElement>>('panel');

  constructor() {
    effect(() => {
      if (this.open()) {
        this.panel()?.nativeElement.focus();
      }
    });
  }

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  protected signed(amount: number): string {
    return amount > 0 ? `+${this.format(amount)}` : this.format(amount);
  }

  protected onEscape(): void {
    if (this.open()) {
      this.closed.emit();
    }
  }
}
