import { Component, computed, inject, input, output, signal } from '@angular/core';
import { LucideSearch } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { RULE_ANCHORS, RuleAnchor } from '@core/rules/rule-anchor.constants';

/**
 * The rulebook's contents, as a column of its own beside the prose.
 *
 * A second navigation rail rather than a block inside the reading column: it stands the whole height
 * of the page, scrolls on its own and stays put while the rules move past it, which is what makes it
 * usable on a document four thousand pixels tall. The page's own sidebar keeps the application's six
 * destinations; this one keeps the rulebook's ten sections.
 *
 * Searches the sections' own text, not their titles: someone typing "rendement" wants the beat
 * called "Chaque match tape fort", and a title-only match would answer that no such rule exists. The
 * index is the dictionary itself (`Translation.searchText`), so nothing is duplicated into a keyword
 * list that would drift from the prose on its first edit.
 *
 * Finds rather than filters: choosing an entry scrolls to that section of a page that always shows
 * all of them. A search that also hid the other nine would leave a reader who scrolled past their
 * answer with no way back to it.
 */
@Component({
  selector: 'app-rule-contents',
  imports: [TranslatePipe, LucideSearch],
  templateUrl: './rule-contents.html',
  host: { class: 'contents' },
})
export class RuleContents {
  private readonly translation = inject(Translation);

  /**
   * Fragment of the section currently on screen, marked in the list.
   */
  public readonly activeId = input<string | null>(null);

  /**
   * Emitted when an entry is chosen, with the fragment to scroll to.
   */
  public readonly jump = output<string>();

  /**
   * What the reader has typed. Owned here: it is this rail's own state and nothing outside it reads
   * the query, only the sections it resolves to.
   */
  protected readonly query = signal('');

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

  /**
   * Relays the field's input event as the query string.
   *
   * @param event - The field's `input` event.
   */
  protected onInput(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
  }
}
