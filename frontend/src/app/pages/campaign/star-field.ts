import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  viewChild,
} from '@angular/core';

import { svgElement as el } from '@shared/rocket/rocket-drawing';

const WIDTH = 1600;
const HEIGHT = 420;
const STAR_COUNT = 220;
const NIGHT = '#040a11';
const BRAND = '#d9954a';
const HAZE = '#5a96be';

/**
 * The sky behind the road of the planets: a fixed field of stars and two faint nebulae.
 *
 * Drawn once, from a seeded sequence, so the same sky comes back on every visit; drawn by script
 * because two hundred circles are a texture, not a view.
 */
@Component({
  selector: 'app-star-field',
  template: `
    <svg
      #sky
      aria-hidden="true"
      class="absolute inset-0 block size-full"
      preserveAspectRatio="xMidYMid slice"
      viewBox="0 0 1600 420"
    ></svg>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StarField {
  private readonly sky = viewChild.required<ElementRef<SVGSVGElement>>('sky');

  constructor() {
    afterNextRender(() => this.draw(this.sky().nativeElement));
  }

  private draw(svg: SVGSVGElement): void {
    let seed = 20260905;
    const random = (): number => {
      seed = (seed * 1664525 + 1013904223) % 4294967296;
      return seed / 4294967296;
    };
    const frag = document.createDocumentFragment();
    const defs = el('defs');
    const warm = el('radialGradient', { id: 'sky-warm', cx: 0.5, cy: 0.5, r: 0.5 });
    warm.append(
      el('stop', { offset: 0, 'stop-color': BRAND, 'stop-opacity': 0.14 }),
      el('stop', { offset: 1, 'stop-color': BRAND, 'stop-opacity': 0 }),
    );
    const cool = el('radialGradient', { id: 'sky-cool', cx: 0.5, cy: 0.5, r: 0.5 });
    cool.append(
      el('stop', { offset: 0, 'stop-color': HAZE, 'stop-opacity': 0.12 }),
      el('stop', { offset: 1, 'stop-color': HAZE, 'stop-opacity': 0 }),
    );
    defs.append(warm, cool);
    frag.append(defs);
    frag.append(el('rect', { x: 0, y: 0, width: WIDTH, height: HEIGHT, fill: NIGHT }));
    frag.append(el('ellipse', { cx: 720, cy: 200, rx: 420, ry: 200, fill: 'url(#sky-warm)' }));
    frag.append(el('ellipse', { cx: 1300, cy: 120, rx: 380, ry: 200, fill: 'url(#sky-cool)' }));
    for (let i = 0; i < STAR_COUNT; i++) {
      frag.append(
        el('circle', {
          cx: (random() * WIDTH).toFixed(1),
          cy: (random() * HEIGHT).toFixed(1),
          r: random() < 0.1 ? 1.6 : 0.9,
          fill: '#cfe4ee',
          opacity: (0.25 + random() * 0.55).toFixed(2),
        }),
      );
    }
    svg.replaceChildren(frag);
  }
}
