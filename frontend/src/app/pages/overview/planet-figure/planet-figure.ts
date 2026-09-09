import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  viewChild,
} from '@angular/core';

import { createSeededRandom } from '@core/random/seeded-random.utils';
import { svgElement as el } from '@core/svg/svg-element.utils';
import {
  PLANET_COLORS,
  PLANET_CX,
  PLANET_CY,
  PLANET_RADIUS,
  PLANET_SEED,
  PLANET_VIEW_SIZE,
  RELIEF_PATCHES,
  RING_SEGMENTS,
  WOUNDED_MARKS,
} from './planet-figure.constants';

/**
 * The planet of the week: the wounded on its lit face, and the guardian's lines around it.
 *
 * What the page has to make understood in one image: the wounded are out there, and something
 * keeps them from leaving. Hence the two objects — the amber marks on the ground, which are the
 * wounded, and the breakthrough ring around, each destroyed segment a piece of the guardian's hit
 * points. When the ring is empty, the planet is open.
 */
@Component({
  selector: 'app-planet-figure',
  templateUrl: './planet-figure.html',
  styleUrl: './planet-figure.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanetFigure {
  /**
   * Share of the guardian's hit points still standing, in [0, 1].
   */
  public readonly guardianLeft = input.required<number>();

  /**
   * Accessible description of the drawing.
   */
  public readonly label = input('');

  protected readonly viewBox = `0 0 ${PLANET_VIEW_SIZE} ${PLANET_VIEW_SIZE}`;

  private readonly planet = viewChild.required<ElementRef<SVGSVGElement>>('planet');

  constructor() {
    afterRenderEffect(() => {
      this.draw(this.planet().nativeElement, this.guardianLeft());
    });
  }

  /**
   * Rebuilds the whole drawing: globe, relief, shadow, wounded marks, then the ring.
   */
  private draw(svg: SVGSVGElement, guardianLeft: number): void {
    const rn = createSeededRandom(PLANET_SEED);
    const frag = document.createDocumentFragment();

    frag.append(this.buildDefs());
    frag.append(
      el('circle', {
        cx: PLANET_CX,
        cy: PLANET_CY,
        r: PLANET_RADIUS * 1.62,
        fill: 'url(#planet-halo)',
      }),
    );
    frag.append(
      el('circle', { cx: PLANET_CX, cy: PLANET_CY, r: PLANET_RADIUS, fill: 'url(#planet-globe)' }),
    );
    frag.append(this.buildRelief(rn));
    frag.append(...this.buildShadowAndAtmosphere());
    frag.append(this.buildWoundedMarks(rn));
    frag.append(this.buildRing(guardianLeft));

    svg.replaceChildren(frag);
  }

  /**
   * Gradients and clip path. The globe is lit from the top left: without a terminator, a flat
   * disc does not read as a planet.
   */
  private buildDefs(): SVGDefsElement {
    const defs = el('defs');
    const globe = el('radialGradient', { id: 'planet-globe', cx: 0.34, cy: 0.28, r: 0.85 });
    globe.append(
      el('stop', { offset: 0, 'stop-color': '#6a5a48' }),
      el('stop', { offset: 0.45, 'stop-color': '#3d3629' }),
      el('stop', { offset: 0.8, 'stop-color': '#1b1e1f' }),
      el('stop', { offset: 1, 'stop-color': '#0a1016' }),
    );
    const halo = el('radialGradient', { id: 'planet-halo', cx: 0.5, cy: 0.5, r: 0.5 });
    halo.append(
      el('stop', { offset: 0.62, 'stop-color': PLANET_COLORS.brand, 'stop-opacity': 0 }),
      el('stop', { offset: 0.78, 'stop-color': PLANET_COLORS.brand, 'stop-opacity': 0.14 }),
      el('stop', { offset: 1, 'stop-color': PLANET_COLORS.brand, 'stop-opacity': 0 }),
    );
    const clip = el('clipPath', { id: 'planet-disc' });
    clip.append(el('circle', { cx: PLANET_CX, cy: PLANET_CY, r: PLANET_RADIUS }));
    defs.append(globe, halo, clip);
    return defs;
  }

  /**
   * Relief: dark and pale patches cut to the disc.
   */
  private buildRelief(rn: () => number): SVGGElement {
    const crust = el('g', { 'clip-path': 'url(#planet-disc)' });
    for (let i = 0; i < RELIEF_PATCHES; i++) {
      const a = rn() * Math.PI * 2;
      const d = Math.sqrt(rn()) * PLANET_RADIUS * 0.88;
      crust.append(
        el('ellipse', {
          cx: (PLANET_CX + Math.cos(a) * d).toFixed(1),
          cy: (PLANET_CY + Math.sin(a) * d * 0.82).toFixed(1),
          rx: (10 + rn() * 26).toFixed(1),
          ry: (6 + rn() * 13).toFixed(1),
          fill: rn() < 0.55 ? '#000000' : '#8a755a',
          opacity: (0.1 + rn() * 0.13).toFixed(2),
          transform: `rotate(${(rn() * 60 - 30).toFixed(0)} ${PLANET_CX} ${PLANET_CY})`,
        }),
      );
    }
    return crust;
  }

  /**
   * The shadow taking the right edge, and the thin warm atmosphere line.
   */
  private buildShadowAndAtmosphere(): readonly SVGCircleElement[] {
    const shadow = el('circle', {
      cx: PLANET_CX + 26,
      cy: PLANET_CY + 20,
      r: PLANET_RADIUS,
      fill: PLANET_COLORS.night,
      opacity: 0.42,
      'clip-path': 'url(#planet-disc)',
    });
    const atmosphere = el('circle', {
      cx: PLANET_CX,
      cy: PLANET_CY,
      r: PLANET_RADIUS + 1,
      fill: 'none',
      stroke: PLANET_COLORS.warm,
      'stroke-width': 1,
      opacity: 0.28,
    });
    return [shadow, atmosphere];
  }

  /**
   * The wounded: marks laid on the lit face, breathing. They do not count the wounded one by one
   * — the figure is written beside — they say there are people there.
   */
  private buildWoundedMarks(rn: () => number): SVGGElement {
    const marks = el('g');
    for (let i = 0; i < WOUNDED_MARKS; i++) {
      const a = rn() * Math.PI * 2;
      const d = Math.sqrt(rn()) * PLANET_RADIUS * 0.74;
      const px = PLANET_CX + Math.cos(a) * d;
      const py = PLANET_CY + Math.sin(a) * d * 0.9;
      if (px - PLANET_CX > 34 && py - PLANET_CY > 24) {
        continue; // nothing in the shadow
      }
      const mark = el('circle', {
        cx: px.toFixed(1),
        cy: py.toFixed(1),
        r: 2.6,
        fill: PLANET_COLORS.warmCore,
      });
      mark.append(
        el('animate', {
          attributeName: 'opacity',
          values: '0.95;0.35;0.95',
          dur: `${(2.4 + rn() * 2.6).toFixed(1)}s`,
          repeatCount: 'indefinite',
        }),
      );
      marks.append(mark);
      marks.append(
        el('circle', {
          cx: px.toFixed(1),
          cy: py.toFixed(1),
          r: 7,
          fill: PLANET_COLORS.warm,
          opacity: 0.1,
        }),
      );
    }
    return marks;
  }

  /**
   * The breakthrough. One segment per share of the hit points: those left are standing, in red;
   * the others drift outward, extinguished. The same value as the bar under the planet, said in
   * an image.
   */
  private buildRing(guardianLeft: number): SVGGElement {
    const ring = el('g');
    const held = Math.round(RING_SEGMENTS * Math.max(0, Math.min(1, guardianLeft)));
    for (let i = 0; i < RING_SEGMENTS; i++) {
      const angle = (-90 + (i * 360) / RING_SEGMENTS) * (Math.PI / 180);
      const alive = i < held;
      const drift = alive ? 0 : 9 + ((i * 37) % 11);
      const r0 = PLANET_RADIUS + 20 + drift;
      const r1 = r0 + (alive ? 13 : 6);
      const segment = el('line', {
        x1: (PLANET_CX + Math.cos(angle) * r0).toFixed(1),
        y1: (PLANET_CY + Math.sin(angle) * r0).toFixed(1),
        x2: (PLANET_CX + Math.cos(angle) * r1).toFixed(1),
        y2: (PLANET_CY + Math.sin(angle) * r1).toFixed(1),
        stroke: alive ? PLANET_COLORS.segmentAlive : PLANET_COLORS.segmentDead,
        'stroke-width': alive ? 5 : 3,
        'stroke-linecap': 'round',
        opacity: alive ? 0.92 : 0.3,
      });
      if (alive) {
        segment.append(
          el('animate', {
            attributeName: 'opacity',
            values: '0.92;0.6;0.92',
            dur: '3.4s',
            begin: `${(i * 0.11).toFixed(2)}s`,
            repeatCount: 'indefinite',
          }),
        );
      }
      ring.append(segment);
    }
    return ring;
  }
}
