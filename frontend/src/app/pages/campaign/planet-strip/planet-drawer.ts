import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideCheck,
  LucideHeartPulse,
  LucideSkull,
  LucideStar,
  LucideTarget,
  LucideUsers,
  LucideWheat,
  LucideX,
} from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Planet } from '../campaign.model';

/**
 * The report of one planet: three columns whose subjects depend on where the week stands.
 */
@Component({
  selector: 'app-planet-drawer',
  imports: [
    TranslatePipe,
    RouterLink,
    LucideCheck,
    LucideHeartPulse,
    LucideSkull,
    LucideStar,
    LucideTarget,
    LucideUsers,
    LucideWheat,
    LucideX,
  ],
  templateUrl: './planet-drawer.html',
  styleUrl: './planet-drawer.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanetDrawer {
  public readonly planet = input.required<Planet>();

  /**
   * Where the pointer sits along the strip, as a CSS length.
   */
  public readonly pointerX = input.required<string>();

  public readonly closed = output();

  private readonly translation = inject(Translation);

  protected close(): void {
    this.closed.emit();
  }

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  protected signed(amount: number): string {
    const sign = amount > 0 ? '+' : amount < 0 ? '−' : '';
    return `${sign}${this.format(Math.abs(amount))}`;
  }

  /**
   * Whether a settled week's report should show the base at all.
   */
  protected hasBase(population: number | null): population is number {
    return population !== null;
  }
}
