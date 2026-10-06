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

import { SCENE_HEADROOM } from './base-scene.constants';
import { readSeenPopulation, writeSeenPopulation } from './base-scene.utils';
import { CLOCK_TICK_MS, TOWN_HEIGHT, TOWN_WIDTH } from './town/town-scene.constants';
import { buildTownScene } from './town/town-scene.utils';

/**
 * Colony town drawn as an SVG skyline that grows with the population.
 */
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
   * Guardians defeated so far, one rocket stage each.
   */
  public readonly stagesDone = input.required<number>();

  /**
   * Population a full campaign should reach: the scale the city grows on.
   */
  public readonly fullCampaignPopulation = input.required<number>();

  /**
   * Accessible description of the drawing.
   */
  public readonly label = input('');

  /**
   * Remembers the population between visits so new buildings rise (overview only).
   */
  public readonly trackVisits = input(false);

  /**
   * Drawing frame, raised by the headroom so tall buildings are not clipped.
   */
  protected readonly viewBox = `0 -${SCENE_HEADROOM} ${TOWN_WIDTH} ${TOWN_HEIGHT + SCENE_HEADROOM}`;

  /**
   * SVG element the town scene is drawn into.
   */
  private readonly town = viewChild.required<ElementRef<SVGSVGElement>>('town');

  /**
   * Viewer's clock driving the daylight, refreshed every few minutes.
   */
  private readonly now = signal(Date.now());

  /**
   * Population of the previous drawing, null until a known one was drawn.
   */
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

      // Zero means still loading: keeping it would raise the whole city next time.
      if (population > 0) {
        this.drawnPopulation = population;
        if (this.trackVisits()) {
          writeSeenPopulation(population);
        }
      }
    });
  }
}
