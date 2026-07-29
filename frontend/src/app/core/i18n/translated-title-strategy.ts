import { effect, inject, Injectable, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';

import { Translation } from './translation';

/**
 * Suffix appended to every page title, so the browser tab identifies the application even when
 * several are open.
 */
const APPLICATION_NAME = 'Valorant Quests';

/**
 * Sets the document title from each route's `title`, treated as a translation key.
 *
 * Angular's default strategy writes the route's `title` verbatim, which would leave the tab in a
 * single hard-coded language. This strategy resolves the key against the active dictionary and
 * re-applies it whenever the language changes, keeping the tab consistent with the rendered page.
 */
@Injectable()
export class TranslatedTitleStrategy extends TitleStrategy {
  /**
   * Document title service used to write the resolved title.
   */
  private readonly title = inject(Title);

  /**
   * i18n service providing the active dictionary.
   */
  private readonly translation = inject(Translation);

  /**
   * Translation key of the active route, or `undefined` for a route declaring no title.
   */
  private readonly titleKey = signal<string | undefined>(undefined);

  /**
   * Re-applies the document title whenever the active route or the loaded dictionary changes.
   */
  constructor() {
    super();

    effect(() => {
      const key = this.titleKey();
      // Reading through the service registers the dictionary signal as a dependency, so the title
      // is rewritten once a new language finishes loading.
      const translated = key ? this.translation.translate(key) : null;

      this.title.setTitle(translated ? `${translated} · ${APPLICATION_NAME}` : APPLICATION_NAME);
    });
  }

  /**
   * Records the translation key of the route being activated.
   *
   * @param snapshot - Snapshot of the router state being activated.
   */
  public override updateTitle(snapshot: RouterStateSnapshot): void {
    this.titleKey.set(this.buildTitle(snapshot));
  }
}
