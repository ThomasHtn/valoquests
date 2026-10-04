import { Component, computed, inject, input, output } from '@angular/core';
import {
  LucideFilter,
  LucideHourglass,
  LucidePlus,
  LucideRefreshCw,
  LucideTriangleAlert,
} from '@lucide/angular';

import { Translation } from '@core/i18n/translation';
import { Connectivity } from '@core/http/connectivity';
import { Button } from '@shared/button/button';
import { EmptyPlate } from '@shared/empty-plate/empty-plate';
import { EmptyPlate as EmptyPlateContent } from '@shared/empty-plate/empty-plate.model';

/**
 * Loading, error, empty or content switch for a resource view; project a `[skeleton]`.
 */
@Component({
  selector: 'app-resource-state',
  imports: [
    Button,
    EmptyPlate,
    LucideFilter,
    LucideHourglass,
    LucidePlus,
    LucideRefreshCw,
    LucideTriangleAlert,
  ],
  templateUrl: './resource-state.html',
  // A column, not `contents`, to keep the page gutter; `gap: inherit` spaces blocks like the stack.
  host: { class: 'flex flex-col [gap:inherit]', '[class.grow]': 'grow()' },
})
export class ResourceState {
  /**
   * Resolves the retry label, identical on every screen.
   */
  private readonly translation = inject(Translation);

  /**
   * Device network status, which decides the error hint.
   */
  private readonly connectivity = inject(Connectivity);

  /**
   * Whether the resource is still loading.
   */
  public readonly isLoading = input.required<boolean>();

  /**
   * Whether the resource failed to load.
   */
  public readonly isError = input.required<boolean>();

  /**
   * Whether the resource loaded successfully but holds no data.
   */
  public readonly isEmpty = input(false);

  /**
   * Translated text shown while loading.
   */
  public readonly loadingText = input.required<string>();

  /**
   * Translated text shown on error.
   */
  public readonly errorText = input.required<string>();

  /**
   * Translated text shown when {@link isEmpty}.
   */
  public readonly emptyText = input('');

  /**
   * Empty state as a mission plate, replacing {@link emptyText} and the hexagon.
   */
  public readonly emptyPlate = input<EmptyPlateContent | null>(null);

  /**
   * Tailwind padding of the error and empty states.
   */
  public readonly padding = input('px-5 py-6');

  /**
   * Kind of emptiness, deciding glyph and tone: `waiting` (data will come), `filter` (clear it),
   * `creation` (project the action), `anomaly` (something failed, announced as an alert).
   */
  public readonly emptyKind = input<'waiting' | 'filter' | 'creation' | 'anomaly'>('filter');

  /**
   * Stretches to the parent's height; only for a projected `flex-1` block that must grow.
   */
  public readonly grow = input(false);

  /**
   * Emitted on retry from the error state; wire it to the resource's `reload()`.
   */
  public readonly retry = output<void>();

  /**
   * Translated retry label, computed to follow language switches.
   */
  protected readonly retryLabel = computed(() => this.translation.translate('retry'));

  /**
   * Translated error hint, which depends on whether the device is offline.
   */
  protected readonly errorHint = computed(() =>
    this.translation.translate(
      this.connectivity.online() ? 'resourceState.errorHint' : 'resourceState.offlineHint',
    ),
  );

  /**
   * Hexagon tint by {@link emptyKind}: danger for `anomaly`, amber for `waiting`.
   */
  protected readonly emptyGlyphClass = computed(() => {
    switch (this.emptyKind()) {
      case 'anomaly':
        return 'bg-danger/15 text-danger';
      case 'waiting':
        return 'bg-brand-500/15 text-brand-500';
      default:
        return 'bg-surface-800 text-text-secondary';
    }
  });
}
