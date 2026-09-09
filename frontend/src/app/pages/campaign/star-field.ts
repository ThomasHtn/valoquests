import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  viewChild,
} from '@angular/core';

import { createSeededRandom } from '@core/random/seeded-random.utils';
import { svgElement as el } from '@core/svg/svg-element.utils';
import {
  BRIGHT_STAR_SHARE,
  SKY_COLORS,
  SKY_HEIGHT,
  SKY_SEED,
  SKY_WIDTH,
  STAR_COUNT,
} from './star-field.constants';

/**
 * The sky behind the road of the planets: a fixed field of stars and two faint nebulae.
 *
 * Drawn once, from a seeded sequence, so the same sky comes back on every visit; drawn by script
 * because two hundred circles are a texture, not a view.
 */
@Component({
  selector: 'app-star-field',
  templateUrl: './star-field.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StarField {
  protected readonly viewBox = `0 0 ${SKY_WIDTH} ${SKY_HEIGHT}`;

  private readonly sky = viewChild.required<ElementRef<SVGSVGElement>>('sky');

  constructor() {
    afterNextRender(() => this.draw(this.sky().nativeElement));
  }

  /**
   * Night plate, two nebulae, then the stars.
   */
  private draw(svg: SVGSVGElement): void {
    const random = createSeededRandom(SKY_SEED);
    const frag = document.createDocumentFragment();

    frag.append(this.buildNebulaeDefs());
    frag.append(
      el('rect', { x: 0, y: 0, width: SKY_WIDTH, height: SKY_HEIGHT, fill: SKY_COLORS.night }),
    );
    frag.append(el('ellipse', { cx: 720, cy: 200, rx: 420, ry: 200, fill: 'url(#sky-warm)' }));
    frag.append(el('ellipse', { cx: 1300, cy: 120, rx: 380, ry: 200, fill: 'url(#sky-cool)' }));

    for (let i = 0; i < STAR_COUNT; i++) {
      frag.append(
        el('circle', {
          cx: (random() * SKY_WIDTH).toFixed(1),
          cy: (random() * SKY_HEIGHT).toFixed(1),
          r: random() < BRIGHT_STAR_SHARE ? 1.6 : 0.9,
          fill: SKY_COLORS.star,
          opacity: (0.25 + random() * 0.55).toFixed(2),
        }),
      );
    }
    svg.replaceChildren(frag);
  }

  /**
   * One warm and one cool radial gradient, each fading to transparent at its edge.
   */
  private buildNebulaeDefs(): SVGDefsElement {
    const defs = el('defs');
    const warm = el('radialGradient', { id: 'sky-warm', cx: 0.5, cy: 0.5, r: 0.5 });
    warm.append(
      el('stop', { offset: 0, 'stop-color': SKY_COLORS.brand, 'stop-opacity': 0.14 }),
      el('stop', { offset: 1, 'stop-color': SKY_COLORS.brand, 'stop-opacity': 0 }),
    );
    const cool = el('radialGradient', { id: 'sky-cool', cx: 0.5, cy: 0.5, r: 0.5 });
    cool.append(
      el('stop', { offset: 0, 'stop-color': SKY_COLORS.haze, 'stop-opacity': 0.12 }),
      el('stop', { offset: 1, 'stop-color': SKY_COLORS.haze, 'stop-opacity': 0 }),
    );
    defs.append(warm, cool);
    return defs;
  }
}
