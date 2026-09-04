import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  viewChild,
} from '@angular/core';

import { buildTownScene, TOWN_HEIGHT, TOWN_WIDTH } from './town-scene.builder';

/**
 * The base at night, and the rocket being built in its middle.
 *
 * The state of the campaign, drawn: the city is the score, the rocket gains a stage per guardian
 * defeated, and what remains to be built stands there, dotted. Drawn imperatively into one
 * `<svg>` rather than templated: a few hundred nodes computed from two numbers are a drawing, not
 * a view, and a template of `@for` loops over generated geometry would say nothing a reader could
 * follow.
 */
@Component({
  selector: 'app-base-scene',
  template: `
    <svg
      #town
      [attr.aria-label]="label()"
      [attr.viewBox]="viewBox"
      class="block aspect-[1200/430] min-h-80 w-full max-h-[40rem]"
      preserveAspectRatio="xMidYMax slice"
      role="img"
    ></svg>
  `,
  styles: `
    :host {
      display: block;
    }
  `,
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

  protected readonly viewBox = `0 0 ${TOWN_WIDTH} ${TOWN_HEIGHT}`;

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
