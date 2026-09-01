import { Component, computed, ElementRef, inject, input, output, signal } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { LucideChevronDown, LucideSearch, LucideX } from '@lucide/angular';

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
 * Below `lg` the same two pieces are laid out as a band under the page's context bar — the field on
 * its own line, the sections behind a trigger that drops them over the page. The rail was simply not
 * rendered there, which left the search as a feature of desks only, on the one screen size where
 * scrolling to find a rule costs the most.
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
   * Paint of the entry the reader is standing in, and of the nine others.
   *
   * Both are the application rail's own (see `sidebar.html` and `navActiveClass`), down to the
   * gradient and the tint: this column stands next to that one, and a reader moving between them
   * must not have to learn a second vocabulary for "here" and "under the pointer".
   *
   * Held as two strings rather than as five `[class.x]` bindings on the button: the entry is
   * rendered twice — once in the rail, once in the phone's panel — and a state spread over that
   * many bindings is exactly what drifts between two copies of the same markup.
   */
  protected readonly activeEntryClass =
    'bg-linear-to-r from-brand-500/20 to-transparent text-brand-500 before:bg-brand-500';

  protected readonly idleEntryClass =
    'text-text-secondary hover:bg-brand-500/8 hover:text-text-primary';

  /**
   * `id` of the phone's contents panel, referenced by its trigger's `aria-controls`.
   */
  protected readonly panelId = 'rule-contents-panel';

  /**
   * What the reader has typed. Owned here: it is this rail's own state and nothing outside it reads
   * the query, only the sections it resolves to.
   */
  protected readonly query = signal('');

  /**
   * Whether the phone's contents panel is dropped. Never read above `lg`, where the list is always
   * on screen.
   */
  protected readonly open = signal(false);

  private readonly translation = inject(Translation);

  /**
   * Host element, used to tell a click inside this band from one on the page behind it.
   */
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

  /**
   * Relays the field's input event as the query string.
   *
   * @param event - The field's `input` event.
   */
  protected onInput(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
  }

  /**
   * Empties the field and puts the caret back in it, so clearing a search leaves the reader ready to
   * type the next one rather than having to aim at the field again.
   *
   * The field is handed in rather than looked up: both shapes of the band are in the DOM at all
   * times, one of them hidden, and a query for the first `input` would half the time refocus the one
   * the reader cannot see.
   *
   * @param field - The field the clear button belongs to.
   */
  protected clear(field: HTMLInputElement): void {
    this.query.set('');
    field.focus();
  }

  /**
   * Drops the phone's list, and marks the section chosen.
   *
   * @param anchor - Fragment of the section to reveal.
   */
  protected choose(anchor: string): void {
    this.close();
    this.jump.emit(anchor);
  }

  /**
   * Shows the matches as soon as the field is used, without a second tap on the trigger — typing
   * into a search that answers nowhere visible is the one state this band must not have.
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
   * Closes the phone's list when the reader touches the rulebook behind it.
   *
   * Bound on the document rather than on the scrim alone: the panel is capped at 60% of the screen,
   * so on a long phone there is page left uncovered beside it, and a tap there has to dismiss it too.
   *
   * @param event - The document's `click` event.
   */
  protected onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }
}
