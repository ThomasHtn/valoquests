import { Component, computed, inject, input, output } from '@angular/core';

import {
  LucideFilter,
  LucideHourglass,
  LucidePlus,
  LucideRefreshCw,
  LucideTriangleAlert,
} from '@lucide/angular';

import { Connectivity } from '@core/http/connectivity';
import { Translation } from '@core/i18n/translation';
import { Button } from '@shared/button/button';
import { EmptyPlate } from '@shared/empty-plate/empty-plate';
import { EmptyPlate as EmptyPlateContent } from '@shared/empty-plate/empty-plate.model';

import { ResourceStatePadding } from './resource-state.model';

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
  styleUrl: './resource-state.scss',
  host: { '[class.resource-state--grow]': 'grow()' },
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
   * Padding of the fallback, error and empty states, after where the state sits.
   */
  public readonly padding = input<ResourceStatePadding>('card');

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
   * Padding modifier of the fallback, error and empty states.
   */
  protected readonly frameClass = computed(() => `resource-state__frame--${this.padding()}`);

  /**
   * Hexagon tint modifier by {@link emptyKind}: danger for `anomaly`, amber for `waiting`, neutral otherwise.
   */
  protected readonly emptyGlyphClass = computed(() => {
    switch (this.emptyKind()) {
      case 'anomaly':
        return 'resource-state__glyph--danger';
      case 'waiting':
        return 'resource-state__glyph--brand';
      default:
        return '';
    }
  });
}
