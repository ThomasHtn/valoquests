import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { LucideCheck, LucideStar, LucideSwords, LucideX } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Planet } from '../campaign.model';
import { PlanetDrawer } from './planet-drawer';
import { PlanetOrb } from './planet-orb';

/**
 * Ten planets in a line and the drawer under them.
 *
 * The drawer is always open: on the week's planet at rest, on the planet clicked afterwards,
 * pointing at it; the positions are not data, the grid gives them. On a phone the strip scrolls
 * sideways and the pointer goes, the drawer then simply follows the strip.
 */
@Component({
  selector: 'app-planet-strip',
  imports: [TranslatePipe, PlanetOrb, PlanetDrawer, LucideCheck, LucideStar, LucideSwords, LucideX],
  templateUrl: './planet-strip.html',
  styleUrl: './planet-strip.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanetStrip {
  public readonly planets = input.required<readonly Planet[]>();

  /**
   * Index of the planet whose report is open; `null` only until the planets arrive.
   */
  protected readonly openedIndex = signal<number | null>(null);

  /**
   * The planet the campaign stands on: the last one reached. The week's planet reads `won` once
   * its guardian is down, so `now` alone would miss it.
   */
  private readonly currentIndex = computed<number | null>(() => {
    const reached = this.planets().filter((planet) => planet.state !== 'ahead');
    return reached.at(-1)?.index ?? this.planets()[0]?.index ?? null;
  });

  protected readonly opened = computed<Planet | null>(() => {
    const index = this.openedIndex();
    return this.planets().find((planet) => planet.index === index) ?? null;
  });

  /**
   * Share of the strip's width the pointer sits at, under the opened planet.
   */
  protected readonly pointerX = computed(() => {
    const index = this.openedIndex();
    const count = this.planets().length;
    return index === null || count === 0 ? '50%' : `${((index - 0.5) / count) * 100}%`;
  });

  private readonly wrap = viewChild.required<ElementRef<HTMLElement>>('wrap');

  constructor() {
    // The week's planet opens by itself, once, when the planets arrive with the campaign.
    effect(() => {
      const current = this.currentIndex();
      if (this.openedIndex() === null && current !== null) {
        this.openedIndex.set(current);
      }
    });

    // On a phone the strip scrolls: it opens on the last planet reached, not on the first one.
    // The planets arrive with the campaign, after the first render, so this waits for them and
    // then runs once.
    let scrolled = false;
    afterRenderEffect(() => {
      if (scrolled || this.planets().length === 0) {
        return;
      }
      scrolled = true;
      const wrap = this.wrap().nativeElement;
      const reached = [...wrap.querySelectorAll<HTMLElement>('.pl:not(.pl--ahead)')];
      const current = reached.at(-1) ?? null;
      if (current && wrap.scrollWidth > wrap.clientWidth) {
        current.scrollIntoView({ inline: 'center', block: 'nearest' });
      }
    });
  }

  protected open(planet: Planet): void {
    this.openedIndex.set(planet.index);
  }
}
