import { Component, computed, input } from '@angular/core';

import { ColonyTierGlyph } from '@core/colony/colony-view.model';

/**
 * One building of a silhouette: its height, its roofline, and whether it carries the band's
 * scaffold — always its tallest building, the one still being built towards the next step.
 */
interface BuildingConfig {
  readonly heightPx: number;
  readonly roof: 'pitched' | 'spire' | 'flat';
}

/**
 * One building resolved into everything the template draws: its shape and how many of its
 * windows are lit.
 */
interface BuildingView extends BuildingConfig {
  readonly litWindows: number;
  readonly hasScaffold: boolean;
}

/**
 * Windows drawn per building, regardless of band — the grid the mock-up's `.windows` class lays
 * out (two columns, three rows).
 */
const WINDOWS_PER_BUILDING = 6;

/**
 * The four fixed silhouettes, one per {@link ColonyTierGlyph}. Heights climb within each band and
 * again from band to band, so the four read as one town growing rather than four unrelated
 * drawings. The CAMP band matches the direction's own validated mock-up pixel for pixel; the other
 * three carry its proportions forward — there is no illustration further up the ladder to match yet.
 */
const BAND_BUILDINGS: Readonly<Record<ColonyTierGlyph, readonly BuildingConfig[]>> = {
  CAMP: [
    { heightPx: 34, roof: 'pitched' },
    { heightPx: 42, roof: 'pitched' },
    { heightPx: 28, roof: 'pitched' },
    { heightPx: 38, roof: 'pitched' },
    { heightPx: 34, roof: 'pitched' },
    { heightPx: 46, roof: 'pitched' },
  ],
  HOUSES: [
    { heightPx: 52, roof: 'flat' },
    { heightPx: 72, roof: 'pitched' },
    { heightPx: 58, roof: 'flat' },
    { heightPx: 82, roof: 'pitched' },
    { heightPx: 62, roof: 'flat' },
    { heightPx: 92, roof: 'flat' },
  ],
  SKYLINE: [
    { heightPx: 82, roof: 'flat' },
    { heightPx: 114, roof: 'flat' },
    { heightPx: 98, roof: 'flat' },
    { heightPx: 130, roof: 'flat' },
    { heightPx: 104, roof: 'flat' },
    { heightPx: 144, roof: 'flat' },
  ],
  MONUMENT: [
    { heightPx: 95, roof: 'flat' },
    { heightPx: 124, roof: 'flat' },
    { heightPx: 108, roof: 'flat' },
    { heightPx: 140, roof: 'flat' },
    { heightPx: 114, roof: 'flat' },
    { heightPx: 180, roof: 'spire' },
  ],
};

/**
 * The town, drawn as a growing silhouette rather than a number in a hexagon — see
 * design-review.md §3.1 and §1 (« l'objectif du jeu, rendu visible »). Deliberately plain CSS, no
 * drawing library: the shapes are six boxes and a handful of `clip-path`/gradient tricks, in the
 * same register as `clip-hex`/`notch-tr` elsewhere in the design system, and the band's identity
 * carries a fixed, hand-authored silhouette rather than one procedurally laid out per tier — twelve
 * tiers reduced to four bands is the whole point (see {@link ColonyTierGlyph}).
 *
 * Every building lights its windows before the next one starts, left to right — the read is "the
 * town, mostly lit, thinning out towards its newest edge", not a scatter of sparse buildings.
 */
@Component({
  selector: 'app-town-silhouette',
  templateUrl: './town-silhouette.html',
  styleUrl: './town-silhouette.css',
})
export class TownSilhouette {
  /**
   * Which of the four silhouettes to draw.
   */
  public readonly glyph = input.required<ColonyTierGlyph>();

  /**
   * Share of windows lit, `0`–`100`.
   */
  public readonly populationPercentage = input.required<number>();

  /**
   * Share of the way to the next step, `0`–`100` — the scaffold shrinks as this climbs.
   */
  public readonly progressPercentage = input.required<number>();

  /**
   * The band's buildings, each resolved with how many of its windows are lit.
   *
   * Lit left to right, one building filled before the next starts, rather than each building
   * getting its own share of the percentage — a fractional light on every building would read as
   * "half-built everywhere" instead of "built here, not yet there", which is the point of a
   * skyline over a single gauge.
   */
  protected readonly buildings = computed<readonly BuildingView[]>(() => {
    const configs = BAND_BUILDINGS[this.glyph()];
    const totalWindows = configs.length * WINDOWS_PER_BUILDING;
    const litTotal = Math.round((this.populationPercentage() / 100) * totalWindows);
    let remaining = litTotal;

    return configs.map((config, index) => {
      const lit = Math.max(0, Math.min(WINDOWS_PER_BUILDING, remaining));
      remaining -= lit;

      return {
        ...config,
        litWindows: lit,
        hasScaffold: index === configs.length - 1,
      };
    });
  });

  /**
   * Share of the scaffolded building still left to build, `0`–`100` — the complement of
   * {@link progressPercentage}: the scaffold covers what remains, and shrinks as the step is paid.
   */
  protected readonly scaffoldPercentage = computed(() =>
    Math.max(0, Math.min(100, 100 - this.progressPercentage())),
  );

  /**
   * Fixed window indices to iterate in the template — a plain array has no identity `@for` can
   * track by, so each building repeats this same six-item list.
   */
  protected readonly windowSlots: readonly number[] = Array.from(
    { length: WINDOWS_PER_BUILDING },
    (_, index) => index,
  );
}
