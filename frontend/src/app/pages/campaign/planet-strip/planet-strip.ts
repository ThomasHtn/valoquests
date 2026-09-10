import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  ElementRef,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import {
  LucideCheck,
  LucideChevronLeft,
  LucideChevronRight,
  LucideStar,
  LucideSwords,
  LucideX,
} from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Breakpoint } from '@core/viewport/breakpoint';
import { Planet } from '../campaign.model';
import { PlanetDrawer } from './planet-drawer';
import { PlanetOrb } from './planet-orb';

/**
 * Ten planets in a line and the drawer under them.
 *
 * The drawer is always open: on the week's planet at rest, on the planet clicked afterwards,
 * pointing at it; the positions are not data, the grid gives them. Below `md` the strip becomes a
 * slider holding one planet per screen: the road between the planets no longer fits, so the rail
 * of ten ticks under the slider carries the progress and jumps to a week, and the drawer follows
 * whatever planet is centred.
 */
@Component({
  selector: 'app-planet-strip',
  imports: [
    TranslatePipe,
    PlanetOrb,
    PlanetDrawer,
    LucideCheck,
    LucideChevronLeft,
    LucideChevronRight,
    LucideStar,
    LucideSwords,
    LucideX,
  ],
  templateUrl: './planet-strip.html',
  styleUrl: './planet-strip.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanetStrip {
  public readonly planets = input.required<readonly Planet[]>();

  private readonly breakpoint = inject(Breakpoint);

  /**
   * Index of the planet whose report is open; `null` only until the planets arrive.
   */
  protected readonly openedIndex = signal<number | null>(null);

  /**
   * Whether the strip runs as a one-per-screen slider rather than as ten planets in a line.
   */
  protected readonly isSlider = computed(() => !this.breakpoint.isMedium());

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
   * Position of the opened planet in the strip, `-1` while none is opened.
   */
  protected readonly openedPosition = computed(() =>
    this.planets().findIndex((planet) => planet.index === this.openedIndex()),
  );

  /**
   * Share of the strip's width the pointer sits at, under the opened planet. The slider centres
   * its planet, so the pointer sits in the middle there.
   */
  protected readonly pointerX = computed(() => {
    const count = this.planets().length;
    const position = this.openedPosition();
    return this.isSlider() || position < 0 || count === 0
      ? '50%'
      : `${((position + 0.5) / count) * 100}%`;
  });

  private readonly wrap = viewChild.required<ElementRef<HTMLElement>>('wrap');

  /**
   * Guard against reading the scroll offset on every scroll event: one read per frame is enough
   * to follow a swipe.
   */
  private syncing = false;

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

  /**
   * Follows the swipe: the report under the slider is the one of the centred planet.
   */
  protected onScroll(): void {
    if (!this.isSlider() || this.syncing) {
      return;
    }
    this.syncing = true;
    requestAnimationFrame(() => {
      this.syncing = false;
      const centred = this.centredPosition();
      const planet = centred === null ? null : this.planets()[centred];
      if (planet) {
        this.openedIndex.set(planet.index);
      }
    });
  }

  /**
   * Moves the slider by one planet, from the opened one.
   */
  protected step(delta: number): void {
    const position = this.openedPosition();
    const target = this.planets()[position + delta];
    if (target) {
      this.goTo(target);
    }
  }

  protected goTo(planet: Planet): void {
    this.openedIndex.set(planet.index);
    const position = this.planets().indexOf(planet);
    const slides = this.wrap().nativeElement.querySelectorAll<HTMLElement>('.strip > li');
    slides[position]?.scrollIntoView({
      inline: 'center',
      block: 'nearest',
      behavior: this.prefersReducedMotion() ? 'auto' : 'smooth',
    });
  }

  /**
   * Position of the slide sitting closest to the middle of the viewport.
   */
  private centredPosition(): number | null {
    const wrap = this.wrap().nativeElement;
    const slides = [...wrap.querySelectorAll<HTMLElement>('.strip > li')];
    if (slides.length === 0) {
      return null;
    }
    const centre = wrap.scrollLeft + wrap.clientWidth / 2;
    let best = 0;
    let bestGap = Infinity;
    slides.forEach((slide, position) => {
      const gap = Math.abs(slide.offsetLeft + slide.offsetWidth / 2 - centre);
      if (gap < bestGap) {
        bestGap = gap;
        best = position;
      }
    });
    return best;
  }

  private prefersReducedMotion(): boolean {
    return (
      typeof window !== 'undefined' &&
      !!window.matchMedia &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches
    );
  }
}
