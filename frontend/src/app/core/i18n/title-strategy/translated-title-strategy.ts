import { effect, inject, Service, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { Translation } from '../translation';
import { APPLICATION_NAME } from './translated-title-strategy.constants';

/**
 * Document title from the route's `title` as a translation key, following the language.
 * Not auto-provided: `app.config.ts` registers it as the router's `TitleStrategy`.
 */
@Service({ autoProvided: false })
export class TranslatedTitleStrategy extends TitleStrategy {
  /**
   * Browser title service the translated title is written to.
   */
  private readonly title = inject(Title);

  /**
   * Translation service that resolves the route's title key.
   */
  private readonly translation = inject(Translation);

  /**
   * Title key of the active route, `undefined` when it declares none.
   */
  private readonly titleKey = signal<string | undefined>(undefined);

  constructor() {
    super();

    effect(() => {
      const key = this.titleKey();
      // Tracks the dictionary, so the title is rewritten once a new language loads.
      const translated = key ? this.translation.translate(key) : null;

      this.title.setTitle(translated ? `${translated} · ${APPLICATION_NAME}` : APPLICATION_NAME);
    });
  }

  /**
   * Records the title key of the route being activated.
   */
  public override updateTitle(snapshot: RouterStateSnapshot): void {
    this.titleKey.set(this.buildTitle(snapshot));
  }
}
