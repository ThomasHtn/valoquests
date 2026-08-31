import { Component, computed, input } from '@angular/core';

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
   * Everything the template draws, rebuilt when the step or the population changes.
   */
  protected readonly scene = computed<TownScene>(() => buildTownScene(this.step(), this.life()));

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
    }));

    return [
      { shapes: scene.moon, transform: null, className: 'town-moon', delay: null },
      { shapes: scene.dome, transform: null, className: null, delay: null },
      { shapes: this.stars(), transform: null, className: null, delay: null },
      { shapes: scene.backdrop, transform: null, className: null, delay: null },
      { shapes: scene.terraces, transform, className: null, delay: null },
      ...buildings,
      { shapes: scene.flicker, transform, className: 'town-flicker', delay: null },
      { shapes: scene.furniture, transform, className: null, delay: null },
      { shapes: scene.foreground, transform: null, className: null, delay: null },
      {
        shapes: hasWaterAt(this.step()) ? reflectionsFor(scene.buildings) : [],
        transform,
        className: null,
        delay: null,
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
   * Population as the `0`–`1` the windows are lit on.
   */
  private readonly life = computed(
    () => Math.max(0, Math.min(100, this.populationPercentage())) / 100,
  );
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
}
