import { NgTemplateOutlet } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { LucideChevronDown, LucideChevronLeft, LucideChevronRight } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Avatar } from '@shared/avatar/avatar';
import { WeekOption } from '../leaderboard.model';

let nextId = 0;

/**
 * The week the board shows, and the way to any other: arrows to step through them one at a time,
 * and a listbox naming every week at once — its place in its campaign, its dates, who won it.
 *
 * A listbox rather than a date picker: the board only ever shows a Monday it has a ranking for,
 * so the choice is one of a short list, never a free date.
 */
@Component({
  selector: 'app-week-picker',
  imports: [
    NgTemplateOutlet,
    TranslatePipe,
    Avatar,
    LucideChevronDown,
    LucideChevronLeft,
    LucideChevronRight,
  ],
  templateUrl: './week-picker.html',
  styleUrl: './week-picker.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'relative flex items-center gap-1',
    '(document:click)': 'onDocumentClick($event)',
    '(document:keydown.escape)': 'close()',
  },
})
export class WeekPicker {
  /**
   * Every week the board can show, newest first.
   */
  public readonly options = input.required<readonly WeekOption[]>();

  /**
   * Monday of the week on screen.
   */
  public readonly selected = input.required<string | null>();

  public readonly selectedChange = output<string>();

  protected readonly listboxId = `week-picker-${nextId++}`;

  protected readonly isOpen = signal(false);

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  protected readonly current = computed(
    () => this.options().find((option) => option.weekStart === this.selected()) ?? null,
  );

  private readonly selectedIndex = computed(() =>
    this.options().findIndex((option) => option.weekStart === this.selected()),
  );

  protected readonly canGoBack = computed(
    () => this.selectedIndex() >= 0 && this.selectedIndex() < this.options().length - 1,
  );

  protected readonly canGoForward = computed(() => this.selectedIndex() > 0);

  protected toggle(): void {
    this.isOpen.update((open) => !open);
  }

  protected close(): void {
    this.isOpen.set(false);
  }

  /**
   * Steps to the neighbouring week. Out-of-range steps are ignored rather than clamped, since the
   * arrows are already disabled at both ends.
   *
   * @param offset - `1` to go one week further back, `-1` to come one week forward.
   */
  protected step(offset: number): void {
    const target = this.options()[this.selectedIndex() + offset];
    if (target) {
      this.selectedChange.emit(target.weekStart);
    }
  }

  protected select(option: WeekOption): void {
    this.close();
    this.selectedChange.emit(option.weekStart);
  }

  /**
   * Whether a rule separates this option from the one above it: the two belong to different
   * runs of weeks — one campaign, then none, then an older campaign.
   */
  protected startsGroup(index: number): boolean {
    return index > 0 && this.options()[index - 1].group !== this.options()[index].group;
  }

  protected onDocumentClick(event: MouseEvent): void {
    if (this.isOpen() && !this.host.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }
}
