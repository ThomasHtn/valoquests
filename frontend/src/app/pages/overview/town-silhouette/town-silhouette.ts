import { Component, computed, input } from '@angular/core';

import { ColonyTierName } from '@core/colony/colony.model';
import {
  TownScene,
  TownShape,
  buildTownScene,
  hasWaterAt,
  reflectionsFor,
  townStageFor,
} from './town-scene';

/**
 * The colony, drawn as a place rather than as a figure — see design-review.md §3.1 and §1
 * (« l'objectif du jeu, rendu visible »).
 *
 * A waterfront elevation in full daylight: the rest of the city held back by haze, the colony's own
 * frontage built on three terraces stepping down to the quay, and the water closing the frame. The
 * previous version was a dusk silhouette whose only growth signal was a taller black shape; this one
 * grows by *building different things*, and the step of the ladder decides which. Houses and sheds at
 * the camp, low blocks at the hamlet, glass at the borough, the landmark once it is a great city.
 *
 * The rive grows with them: no water at the camp, then a silt bank, then a masonry quay. So does its
 * crossing — a ford, then a plank walkway on driven piles, then a stone viaduct, then a cable-stayed
 * span. It is the one element of the scene that is not a building, and it is what stops the
 * foreground from reading as a flat band.
 *
 * Morale is the only thing besides the step that changes the drawing, and it changes the weather
 * rather than the hour: a squad that is winning gets a clear sky, one that is losing an overcast
 * one. Morale sets how fast the population climbs, so it belongs in the atmosphere rather than in a
 * tile beside six other tiles.
 */
@Component({
  selector: 'app-town-silhouette',
  templateUrl: './town-silhouette.html',
  styleUrl: './town-silhouette.css',
})
export class TownSilhouette {
  /**
   * Step of the ladder the colony currently stands on, which decides everything the scene builds.
   */
  public readonly tier = input.required<ColonyTierName>();

  /**
   * Morale out of its ceiling, `0`–`100`, which sets how clear the sky is. Defaults to the middle of
   * the range so a caller that has not resolved morale yet still gets a lit scene.
   */
  public readonly moralePercentage = input(50);

  /**
   * Everything the template draws, rebuilt only when the step changes.
   */
  protected readonly scene = computed<TownScene>(() => buildTownScene(townStageFor(this.tier())));

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
      rises: true,
      delay: `${(0.3 + index * 0.075).toFixed(2)}s`,
    }));

    return [
      { shapes: scene.backdrop, transform: null, rises: false, delay: null },
      { shapes: scene.terraces, transform, rises: false, delay: null },
      ...buildings,
      { shapes: scene.furniture, transform, rises: false, delay: null },
      { shapes: scene.foreground, transform: null, rises: false, delay: null },
      {
        shapes: hasWaterAt(townStageFor(this.tier())) ? reflectionsFor(scene.buildings) : [],
        transform,
        rises: false,
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
}

/**
 * One painted group of the scene.
 */
interface TownLayer {
  readonly shapes: readonly TownShape[];

  /** Set on the frontage, its street and its reflections, which are centred together. */
  readonly transform: string | null;

  /** Whether the group rises into place on load. */
  readonly rises: boolean;
  readonly delay: string | null;
}
