import {
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  model,
  signal,
} from '@angular/core';
import { LucideChevronDown, LucideChevronLeft, LucideChevronRight } from '@lucide/angular';

import { nextInstanceId } from '@core/dom/instance-id.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { Season } from '@core/matches/season.model';
import { SeasonPickerOption } from './season-picker.model';
import { buildSeasonPickerOptions } from './season-picker.utils';

/**
 * The profile's season scope, styled after the leaderboard's week picker: arrows stepping one act
 * at a time around a badge naming the act, and a list of every act ruled off by episode.
 *
 * Picks one season, or every season through an empty selection, by default; several at once in
 * `multiple` mode, where the list stays open while the reader ticks acts and never empties.
 */
@Component({
  selector: 'app-season-picker',
  imports: [TranslatePipe, LucideChevronDown, LucideChevronLeft, LucideChevronRight],
  templateUrl: './season-picker.html',
  styleUrl: './season-picker.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'relative flex items-center gap-1',
    '(document:click)': 'onDocumentClick($event)',
    '(document:keydown.escape)': 'close()',
  },
})
export class SeasonPicker {
  /**
   * Every known season, newest first.
   */
  public readonly seasons = input.required<readonly Season[]>();

  /**
   * Selected season identifiers; empty means every season outside `multiple` mode.
   */
  public readonly selection = model<readonly number[]>([]);

  /**
   * Whether several seasons may be held at once.
   */
  public readonly multiple = input(false);

  /**
   * Largest number of seasons held at once in `multiple` mode, or zero for no limit.
   */
  public readonly maxSelection = input(0);

  /**
   * Already-translated note explaining the limit, shown once it is reached.
   */
  public readonly maxSelectionNote = input('');

  /**
   * Accessible name of the control.
   */
  public readonly ariaLabel = input.required<string>();

  /**
   * i18n service, used to spell the seasons out.
   */
  private readonly translation = inject(Translation);

  /**
   * Host element, used to detect clicks outside the open list.
   */
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /**
   * Id of the list, referenced by the trigger's `aria-controls`.
   */
  protected readonly listboxId = nextInstanceId('season-picker');

  /**
   * Whether the list is open.
   */
  protected readonly isOpen = signal(false);

  /**
   * Every season as the picker lists it, newest first.
   */
  protected readonly options = computed(() => {
    this.translation.language();
    return buildSeasonPickerOptions(this.seasons(), (key, params) =>
      this.translation.translate(key, params),
    );
  });

  /**
   * The single season held, or `null` when none or several are.
   */
  protected readonly current = computed<SeasonPickerOption | null>(() => {
    const selection = this.selection();
    return selection.length === 1
      ? (this.options().find((option) => option.id === selection[0]) ?? null)
      : null;
  });

  /**
   * Trigger label when no single season is held: every season, or how many are.
   */
  protected readonly summary = computed(() => {
    this.translation.language();
    const count = this.selection().length;
    return count === 0
      ? this.translation.translate('playerProfile.filters.allSeasons')
      : this.translation.translate('playerProfile.filters.seasonCount', { count });
  });

  /**
   * Position of the single season held in the list, or -1.
   */
  private readonly currentIndex = computed(() => {
    const current = this.current();
    return current ? this.options().indexOf(current) : -1;
  });

  /**
   * Whether an older season can be stepped to.
   */
  protected readonly canGoBack = computed(
    () => this.currentIndex() >= 0 && this.currentIndex() < this.options().length - 1,
  );

  /**
   * Whether a newer season can be stepped to.
   */
  protected readonly canGoForward = computed(() => this.currentIndex() > 0);

  /**
   * Whether the selection has reached {@link maxSelection}.
   */
  protected readonly isAtLimit = computed(() => {
    const limit = this.maxSelection();
    return this.multiple() && limit > 0 && this.selection().length >= limit;
  });

  /**
   * Opens or closes the list.
   */
  protected toggle(): void {
    this.isOpen.update((open) => !open);
  }

  /**
   * Closes the list.
   */
  protected close(): void {
    this.isOpen.set(false);
  }

  /**
   * Steps the single season held to its neighbour.
   *
   * @param offset - 1 for the older season, -1 for the newer one.
   */
  protected step(offset: number): void {
    const target = this.options()[this.currentIndex() + offset];
    if (target) {
      this.selection.set([target.id]);
    }
  }

  /**
   * Picks every season, the empty selection of single mode.
   */
  protected selectAll(): void {
    this.selection.set([]);
    this.close();
  }

  /**
   * Picks a season: replaces the selection in single mode, toggles it in `multiple` mode, where
   * the last season held cannot be dropped.
   *
   * @param option - The season picked.
   */
  protected select(option: SeasonPickerOption): void {
    if (!this.multiple()) {
      this.selection.set([option.id]);
      this.close();
      return;
    }

    const selection = this.selection();
    if (!selection.includes(option.id)) {
      if (!this.isAtLimit()) {
        this.selection.set([...selection, option.id]);
      }
      return;
    }
    if (selection.length > 1) {
      this.selection.set(selection.filter((id) => id !== option.id));
    }
  }

  /**
   * Whether a season is held.
   *
   * @param option - The season.
   * @returns Whether it is part of the selection.
   */
  protected isSelected(option: SeasonPickerOption): boolean {
    return this.selection().includes(option.id);
  }

  /**
   * Whether the list rules a season off from the one above it: the first act of each episode.
   *
   * @param index - Position of the season in the list.
   * @returns Whether a rule goes above it.
   */
  protected startsGroup(index: number): boolean {
    const options = this.options();
    return index > 0 && options[index].era !== options[index - 1].era;
  }

  /**
   * Closes the list on a click outside the control.
   *
   * @param event - The document click.
   */
  protected onDocumentClick(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }
}
