import { Component, computed, inject, input, output } from '@angular/core';
import { LucideInbox, LucideRefreshCw, LucideTriangleAlert } from '@lucide/angular';

import { Translation } from '@core/i18n/translation';

/**
 * Loading / error / empty / content switch for a resource-backed view.
 *
 * Shared by every screen fetching a resource so they all render the exact same loading, error and
 * empty states, differing only by their (already-translated) text. Renders its content through
 * `<ng-content>` once the resource has loaded successfully and is non-empty.
 *
 * Call sites should project a loading placeholder through the `skeleton` attribute, shaped like
 * the content it stands in for. Without one the loading state is visually blank, though it stays
 * announced to assistive technology.
 */
@Component({
  selector: 'app-resource-state',
  imports: [LucideInbox, LucideRefreshCw, LucideTriangleAlert],
  templateUrl: './resource-state.html',
  host: { class: 'contents' },
})
export class ResourceState {
  /**
   * i18n service used to resolve the retry label.
   *
   * Unlike the state messages, which describe the specific resource and are therefore passed in
   * already translated, the retry label is identical on every screen and so belongs here rather
   * than being repeated at each call site.
   */
  private readonly translation = inject(Translation);

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
   * Already-translated text shown while loading.
   */
  public readonly loadingText = input.required<string>();

  /**
   * Already-translated text shown on error.
   */
  public readonly errorText = input.required<string>();

  /**
   * Already-translated text shown when the resource is empty, if {@link isEmpty} can be `true`.
   */
  public readonly emptyText = input('');

  /**
   * Tailwind padding utility applied to the error and empty states, so this component fits both
   * bare-page usages and usages nested in a bordered card.
   */
  public readonly padding = input('px-5 py-6');

  /**
   * Emitted when the user asks to load the resource again from the error state.
   *
   * Call sites are expected to wire this to their resource's `reload()`.
   */
  public readonly retry = output<void>();

  /**
   * Translated label of the retry button.
   *
   * Computed rather than read once, since `translate` resolves against a signal that changes when
   * the dictionary is swapped on a language switch.
   */
  protected readonly retryLabel = computed(() => this.translation.translate('retry'));
}
