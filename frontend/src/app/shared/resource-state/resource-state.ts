import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Loading / error / empty / content switch for a resource-backed view.
 *
 * Shared by every screen fetching a resource so they all render the exact same loading, error and
 * empty states, differing only by their (already-translated) text. Renders its content through
 * `<ng-content>` once the resource has loaded successfully and is non-empty.
 */
@Component({
  selector: 'app-resource-state',
  templateUrl: './resource-state.html',
  host: { class: 'contents' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResourceState {
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
   * Tailwind padding utility applied to each state message, so this component fits both bare-page
   * usages and usages nested in a bordered card.
   */
  public readonly padding = input('px-5 py-6');
}
