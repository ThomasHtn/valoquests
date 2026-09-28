import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { buildTownScene } from './town-scene.utils';
import { CLOCK_TICK_MS, TOWN_HEIGHT, TOWN_WIDTH } from './town-scene.constants';
import { SCENE_HEADROOM } from './base-scene.constants';
import { readSeenPopulation, writeSeenPopulation } from './seen-population.utils';

@Component({
  selector: 'app-base-scene',
  templateUrl: './base-scene.html',
  styleUrl: './base-scene.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BaseScene {
  /**
   * Inhabitants of the base.
   */
  public readonly population = input.required<number>();

  /**
   * Guardians defeated so far, one stage of the rocket each.
   */
  public readonly stagesDone = input.required<number>();

  /**
   * Population a full campaign is expected to reach, the scale the city grows on.
   */
  public readonly fullCampaignPopulation = input.required<number>();

  /**
   * Accessible description of the drawing.
   */
  public readonly label = input('');

  /**
   * Whether to remember the population between visits, so the buildings grown since the last one
   * rise on arrival. Only the overview does: the tour shows the base, it does not follow it.
   */
  public readonly trackVisits = input(false);

  protected readonly viewBox = `0 -${SCENE_HEADROOM} ${TOWN_WIDTH} ${TOWN_HEIGHT + SCENE_HEADROOM}`;

  private readonly town = viewChild.required<ElementRef<SVGSVGElement>>('town');

  // The light follows the viewer's clock; a few minutes between two redraws is invisible in a sky.
  private readonly now = signal(Date.now());

  // Population of the previous drawing, null until one has been made with a known population.
  private drawnPopulation: number | null = null;

  constructor() {
    const timer = setInterval(() => this.now.set(Date.now()), CLOCK_TICK_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));

    afterRenderEffect(() => {
      const population = this.population();
      if (this.drawnPopulation === null && this.trackVisits()) {
        this.drawnPopulation = readSeenPopulation();
      }
      buildTownScene(this.town().nativeElement, {
        population,
        previousPopulation: this.drawnPopulation ?? population,
        stagesDone: this.stagesDone(),
        fullCampaignPopulation: this.fullCampaignPopulation(),
        now: this.now(),
        reducedMotion: matchMedia('(prefers-reduced-motion: reduce)').matches,
      });

      // Zero is the campaign still loading, not a base: keeping it would raise the whole city next.
      if (population > 0) {
        this.drawnPopulation = population;
        if (this.trackVisits()) {
          writeSeenPopulation(population);
        }
      }
    });
  }
}
