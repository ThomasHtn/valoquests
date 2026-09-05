import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  viewChild,
} from '@angular/core';

import { svgElement as el } from '@shared/rocket/rocket-drawing';
import { PlanetState } from '../campaign.model';

const CX = 50;
const CY = 50;
const TONES: Readonly<Record<PlanetState, string>> = {
  won: '#e8ab6b',
  lost: '#e0404e',
  now: '#2dd4bf',
  ahead: '#5b7688',
};

let nextId = 0;

/**
 * One planet of the strip: its ground, a few dark seas, its shading, and the ring of the
 * breakthrough around it. A planet still ahead is a dotted disc: a place, not a world yet.
 */
@Component({
  selector: 'app-planet-orb',
  template: `<svg
    #orb
    aria-hidden="true"
    class="absolute inset-0 size-full"
    viewBox="0 0 100 100"
  ></svg>`,
  host: { class: 'block size-full' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanetOrb {
  public readonly radius = input.required<number>();

  public readonly hue = input.required<string>();

  public readonly state = input.required<PlanetState>();

  /**
   * Share of the guardian's hit points taken, in [0, 1].
   */
  public readonly advance = input.required<number>();

  /**
   * Seed of the seas, so a planet keeps its face from one visit to the next.
   */
  public readonly seed = input.required<number>();

  private readonly orb = viewChild.required<ElementRef<SVGSVGElement>>('orb');

  private readonly id = `orb-${nextId++}`;

  constructor() {
    afterRenderEffect(() => this.draw(this.orb().nativeElement));
  }

  private draw(svg: SVGSVGElement): void {
    const r = this.radius();
    const state = this.state();
    const tone = TONES[state];
    const frag = document.createDocumentFragment();

    if (state === 'ahead') {
      frag.append(
        el('circle', {
          cx: CX,
          cy: CY,
          r,
          fill: '#15222c',
          stroke: '#33495b',
          'stroke-width': 1,
          'stroke-dasharray': '3 3',
        }),
      );
      svg.replaceChildren(frag);
      return;
    }

    const defs = el('defs');
    const shade = el('radialGradient', { id: this.id, cx: 0.32, cy: 0.3, r: 0.8 });
    shade.append(
      el('stop', { offset: 0, 'stop-color': '#ffffff', 'stop-opacity': 0.22 }),
      el('stop', { offset: 0.55, 'stop-color': '#ffffff', 'stop-opacity': 0 }),
      el('stop', { offset: 1, 'stop-color': '#000000', 'stop-opacity': 0.65 }),
    );
    defs.append(shade);
    frag.append(defs);
    frag.append(el('circle', { cx: CX, cy: CY, r, fill: this.hue() }));

    let seed = this.seed() * 977 + 13;
    const random = (): number => {
      seed = (seed * 1664525 + 1013904223) % 4294967296;
      return seed / 4294967296;
    };
    for (let k = 0; k < 5; k++) {
      const angle = random() * Math.PI * 2;
      const distance = Math.sqrt(random()) * r * 0.8;
      frag.append(
        el('ellipse', {
          cx: (CX + Math.cos(angle) * distance).toFixed(1),
          cy: (CY + Math.sin(angle) * distance).toFixed(1),
          rx: (r * 0.12 + random() * r * 0.28).toFixed(1),
          ry: (r * 0.08 + random() * r * 0.16).toFixed(1),
          fill: '#000000',
          opacity: 0.28,
        }),
      );
    }
    frag.append(el('circle', { cx: CX, cy: CY, r, fill: `url(#${this.id})` }));

    // The ring: the guardian's lines, eaten from the top clockwise.
    const ringRadius = r + Math.max(6, r * 0.3);
    const length = 2 * Math.PI * ringRadius;
    frag.append(
      el('circle', {
        cx: CX,
        cy: CY,
        r: ringRadius,
        fill: 'none',
        stroke: 'rgb(4 10 15 / 70%)',
        'stroke-width': 3,
      }),
    );
    frag.append(
      el('circle', {
        cx: CX,
        cy: CY,
        r: ringRadius,
        fill: 'none',
        stroke: tone,
        'stroke-width': 3,
        'stroke-dasharray': `${(length * this.advance()).toFixed(1)} ${length.toFixed(1)}`,
        transform: `rotate(-90 ${CX} ${CY})`,
      }),
    );
    svg.replaceChildren(frag);
  }
}
