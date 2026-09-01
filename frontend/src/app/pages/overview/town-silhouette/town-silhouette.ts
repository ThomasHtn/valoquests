import { Component, computed, input, signal } from '@angular/core';

import {
  TownScene,
  TownShape,
  buildTownScene,
  hasWaterAt,
  reflectionsFor,
  starsFor,
} from './town-scene';

/**
 * The colony, drawn as a place rather than as a figure — see design-review.md §3.1 and §1
 * (« l'objectif du jeu, rendu visible »).
 *
 * A waterfront elevation after dark: the rest of the city held back by haze, the colony's own
 * frontage built on three terraces stepping down to the quay, and the water closing the frame. It
 * grows by *building different things*, and the step of the ladder decides which. Houses and sheds
 * around a fire at the camp, low blocks at the hamlet, glass at the borough, the landmark once it is
 * a great city.
 *
 * The rive grows with them: no water at the camp, then a silt bank, then a masonry quay. So does its
 * crossing — a ford, then a plank walkway on driven piles, then a stone viaduct, then a cable-stayed
 * span carrying traffic. It is the one element of the scene that is not a building, and it is what
 * stops the foreground from reading as a flat band.
 *
 * Past the twelfth step the quay is full and growth moves behind the colony instead: each numbered
 * citadel raises the city on the horizon a notch further, until it closes the sky the camp had all
 * of. Nothing about the run's last weeks used to move the picture at all.
 *
 * Two things besides the step change the drawing, and neither of them changes the hour:
 * {@link populationPercentage} lights the windows, so the squad can see the colony fill up from the
 * inside; {@link moralePercentage} is the weather, which at night is the smog and how many stars get
 * through. A losing week gets a low brown ceiling and sixteen stars, a winning one a deep sky and
 * seventy-eight.
 */
@Component({
  selector: 'app-town-silhouette',
  templateUrl: './town-silhouette.html',
  styleUrl: './town-silhouette.css',
})
export class TownSilhouette {
  /**
   * Step of the ladder the colony stands on, from zero, which decides everything the scene builds.
   *
   * The open-ended step rather than the tier's name: the ladder repeats past its twelfth name as
   * numbered citadels, so a scene keyed on the name freezes for the last third of a strong run. See
   * `tierStepFor`.
   */
  public readonly step = input.required<number>();

  /**
   * Morale out of its ceiling, `0`–`100`, which sets how clear the sky is. Defaults to the middle of
   * the range so a caller that has not resolved morale yet still gets a lit scene.
   */
  public readonly moralePercentage = input(50);

  /**
   * Population out of what the food can feed, `0`–`100`, which sets how many windows are lit.
   *
   * The one figure the daylight version had no way to draw: growth used to be readable only in the
   * outline, so between two steps of the ladder the scene said nothing at all. Now every evening
   * the squad brings in moves it.
   */
  public readonly populationPercentage = input(50);

  /**
   * Share of the roster that has played today, `0`–`100`, which decides how much of the street is
   * lit and how busy the crossing is.
   *
   * The scene's one *daily* reading. The step of the ladder moves about once a week and the windows
   * move by a percent a day, so between two Mondays the drawing used to be the same drawing. Turnout
   * is the figure that is genuinely different from yesterday, and lighting the street with it is
   * also the truthful picture: the evening's harvest is what those people brought in.
   */
  public readonly turnoutPercentage = input(100);

  /**
   * Share of the way to the next step, `0`–`100`, which is how far the quarter under scaffolding has
   * been built.
   *
   * This is what makes the town grow *a little every day* rather than a street at a time. Materials
   * come in whenever a challenge is validated, so this figure moves on most evenings, and the site
   * gains a course of masonry each time. The step landing is then the scaffold coming down and the
   * windows going on, not a building appearing out of nowhere.
   */
  public readonly progressPercentage = input(0);

  /**
   * Everything the template draws, rebuilt when the step, the population or the turnout changes.
   *
   * Deliberately not rebuilt on {@link progressPercentage}: the site is drawn whole and revealed by
   * a clip, so a day's progress costs one custom property and not three thousand shapes.
   */
  protected readonly scene = computed<TownScene>(() =>
    buildTownScene(this.step(), this.life(), this.turnout()),
  );

  /**
   * How much of the site is standing, as the `inset` the template clips it from the top with.
   */
  protected readonly built = computed(
    () => `${(100 - Math.max(0, Math.min(100, this.progressPercentage()))).toFixed(1)}%`,
  );

  /**
   * The stars, kept out of {@link scene} so a change of morale does not rebuild three thousand
   * shapes to put one star out.
   */
  protected readonly stars = computed<readonly TownShape[]>(() => starsFor(this.clarity()));

  /**
   * The scene flattened into the groups the template paints, in order.
   *
   * Each building is its own group so it can rise into place on its own beat; everything else is one
   * group per plane. Only the frontage, its street and its reflections carry the centring transform.
   */
  protected readonly layers = computed<readonly TownLayer[]>(() => {
    const scene = this.scene();
    const transform = `translate(${scene.frontageOffset.toFixed(1)} 0)`;
    const buildings = scene.buildings.map((building, index) => ({
      shapes: building.shapes,
      transform,
      className: 'town-building',
      delay: `${(0.3 + index * 0.075).toFixed(2)}s`,
      clip: null,
    }));

    return [
      { shapes: scene.moon, transform: null, className: 'town-moon', delay: null, clip: null },
      { shapes: scene.dome, transform: null, className: null, delay: null, clip: null },
      { shapes: this.stars(), transform: null, className: 'town-stars', delay: null, clip: null },
      { shapes: scene.backdrop, transform: null, className: null, delay: null, clip: null },
      { shapes: scene.terraces, transform, className: null, delay: null, clip: null },
      ...buildings,
      // Revealed from the ground up by the materials banked so far — see `built`.
      {
        shapes: scene.construction,
        transform,
        className: 'town-site',
        delay: null,
        clip: `inset(${this.built()} 0 0 0) fill-box`,
      },
      { shapes: scene.flicker, transform, className: 'town-flicker', delay: null, clip: null },
      { shapes: scene.furniture, transform, className: null, delay: null, clip: null },
      { shapes: scene.foreground, transform: null, className: null, delay: null, clip: null },
      {
        shapes: hasWaterAt(this.step()) ? reflectionsFor(scene.buildings) : [],
        transform,
        className: null,
        delay: null,
        clip: null,
      },
    ];
  });

  /**
   * Morale as the `0`–`1` the stylesheet interpolates the weather on, clamped so a morale outside
   * its nominal range can never invert the two skies.
   */
  protected readonly clarity = computed(
    () => Math.max(0, Math.min(100, this.moralePercentage())) / 100,
  );

  /**
   * Which of the four skies the horizon wears, read off the visitor's own clock.
   *
   * Only the horizon: the sky's own gradient already carries morale (see {@link clarity}), and
   * putting the hour on it would have overwritten the one thing it says about how the week went. The
   * hour gets the glow at the bottom and the moon instead, which are free.
   *
   * Resolved once, at construction. A page that repainted at midnight would be correcting a detail
   * nobody is watching, at the cost of a timer running for as long as the tab is open.
   */
  protected readonly hour = signal(phaseAt(new Date().getHours()));

  /**
   * Population as the `0`–`1` the windows are lit on.
   */
  private readonly life = computed(
    () => Math.max(0, Math.min(100, this.populationPercentage())) / 100,
  );

  /**
   * Turnout as the `0`–`1` the street is lit on.
   */
  private readonly turnout = computed(
    () => Math.max(0, Math.min(100, this.turnoutPercentage())) / 100,
  );
}

/**
 * The four skies, in the order the day goes through them.
 */
type TownHour = 'dawn' | 'day' | 'dusk' | 'night';

/**
 * Resolves which sky an hour of the day falls under.
 *
 * Exported for its own test rather than inlined: the boundaries are the whole of this decision, and
 * an off-by-one at 7 or at 21 is invisible in a screenshot taken at any other time.
 *
 * @param hour - Hour of the day, `0`–`23`.
 * @returns The sky that hour wears.
 */
export function phaseAt(hour: number): TownHour {
  if (hour < 7 || hour >= 21) {
    return 'night';
  }
  if (hour < 10) {
    return 'dawn';
  }

  return hour < 18 ? 'day' : 'dusk';
}

/**
 * One painted group of the scene.
 */
interface TownLayer {
  readonly shapes: readonly TownShape[];

  /** Set on the frontage, its street and its reflections, which are centred together. */
  readonly transform: string | null;

  /** Class carrying the group's own animation, if it has one. */
  readonly className: string | null;
  readonly delay: string | null;

  /** Set on the site alone, which is revealed from the ground up as it is paid for. */
  readonly clip: string | null;
}
