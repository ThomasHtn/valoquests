import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  viewChild,
} from '@angular/core';
import { buildTownScene } from './town-scene.utils';
import { TOWN_HEIGHT, TOWN_WIDTH } from './town-scene.constants';
import { SCENE_HEADROOM } from './base-scene.constants';

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

  // Headroom above the drawing: the launcher's dotted outline reaches the top of the frame, and
  // on a phone the frame is cropped to its height, so without it the rocket's tip went under the
  // context bar.
  protected readonly viewBox = `0 -${SCENE_HEADROOM} ${TOWN_WIDTH} ${TOWN_HEIGHT + SCENE_HEADROOM}`;

  private readonly town = viewChild.required<ElementRef<SVGSVGElement>>('town');

  constructor() {
    afterRenderEffect(() => {
      buildTownScene(this.town().nativeElement, {
        population: this.population(),
        stagesDone: this.stagesDone(),
        fullCampaignPopulation: this.fullCampaignPopulation(),
        reducedMotion: matchMedia('(prefers-reduced-motion: reduce)').matches,
      });
    });
  }
}
