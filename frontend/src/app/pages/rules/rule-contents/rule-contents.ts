import { Component, computed, ElementRef, inject, input, output, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { LucideChevronDown, LucideSearch, LucideX } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { RULE_ANCHORS, RuleAnchor } from '@core/rules/rule-anchor.constants';

/**
 * The rulebook's contents, as a column of its own beside the prose.
 *
 * A second navigation rail rather than a block inside the reading column: it stands the whole
 * height of the page, scrolls on its own and stays put while the rules move past it. Below `lg`
 * the same two pieces are laid out as a band under the page's context bar: the field on its own
 * line, the sections behind a trigger that drops them over the page.
 *
 * Searches the sections' own text, not their titles: the index is the dictionary itself
 * (`Translation.searchText`), so nothing is duplicated into a keyword list that would drift from
 * the prose on its first edit. Finds rather than filters: choosing an entry scrolls to that section
 * of a page that always shows all of them.
 */
@Component({
  selector: 'app-rule-contents',
  imports: [TranslatePipe, NgTemplateOutlet, LucideChevronDown, LucideSearch, LucideX],
  templateUrl: './rule-contents.html',
  host: {
    class: 'contents',
    '(document:click)': 'onDocumentClick($event)',
    '(keydown.escape)': 'close()',
  },
})
export class RuleContents {
  /**
   * Fragment of the section currently on screen, marked in the list.
   */
  public readonly activeId = input<string | null>(null);

  /**
   * Emitted when an entry is chosen, with the fragment to scroll to.
   */
  public readonly jump = output<string>();

  /**
   * Paint of the entry the reader is standing in, and of the others: the application rail's own,
   * so a reader moving between the two columns meets one vocabulary for "here".
   */
  protected readonly activeEntryClass =
    'bg-linear-to-r from-brand-500/20 to-transparent text-brand-500 before:bg-brand-500';

  protected readonly idleEntryClass =
    'text-text-secondary hover:bg-brand-500/8 hover:text-text-primary';

  /**
   * `id` of the phone's contents panel, referenced by its trigger's `aria-controls`.
   */
  protected readonly panelId = 'rule-contents-panel';

  protected readonly query = signal('');

  /**
   * Whether the phone's contents panel is dropped. Never read above `lg`.
   */
  protected readonly open = signal(false);

  private readonly translation = inject(Translation);

  private readonly elementRef = inject(ElementRef<HTMLElement>);

  /**
   * Sections matching what has been typed, in reading order.
   */
  protected readonly entries = computed<readonly RuleAnchor[]>(() => {
    const needle = this.query().trim().toLowerCase();
    if (needle === '') {
      return RULE_ANCHORS;
    }

    // Read so the list re-matches when the dictionary is swapped on a language switch.
    this.translation.language();

    return RULE_ANCHORS.filter((anchor) =>
      this.translation.searchText(`rules.sections.${anchor.key}`).includes(needle),
    );
  });

  protected onInput(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
  }

  /**
   * Empties the field and puts the caret back in it. The field is handed in rather than looked up:
   * both shapes of the band are in the DOM, one hidden, and a query for the first `input` would
   * half the time refocus the one the reader cannot see.
   *
   * @param field - The field the clear button belongs to.
   */
  protected clear(field: HTMLInputElement): void {
    this.query.set('');
    field.focus();
  }

  protected choose(anchor: string): void {
    this.close();
    this.jump.emit(anchor);
  }

  /**
   * Shows the matches as soon as the field is used, without a second tap on the trigger.
   */
  protected reveal(): void {
    this.open.set(true);
  }

  protected toggle(): void {
    this.open.update((open) => !open);
  }

  protected close(): void {
    this.open.set(false);
  }

  /**
   * Closes the phone's list when the reader touches the rulebook behind it. On the document rather
   * than on the scrim alone: the panel is capped at 60% of the screen and the band stays uncovered.
   *
   * @param event - The document's `click` event.
   */
  protected onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }
}
