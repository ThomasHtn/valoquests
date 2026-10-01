import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  viewChild,
} from '@angular/core';

import { PLANET_ART_DISC_RATIO, PLANET_ART_LIT_FACE } from '@core/campaign/planet-art.constants';
import { isRingedPlanet, resolvePlanetArtUrl } from '@core/campaign/planet-art.utils';
import { createSeededRandom } from '@core/random/seeded-random.utils';
import { svgElement as el } from '@core/svg/svg-element.utils';
import {
  PLANET_ART_SIDE,
  PLANET_COLORS,
  PLANET_CX,
  PLANET_CY,
  PLANET_SEED,
  PLANET_VIEW_SIZE,
  RING_INNER_RADIUS,
  RING_SEGMENTS,
  RINGED_PLANET_ART_SIDE,
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
   * One-based week of the planet, which picks its drawing: the same one as on the frieze.
   */
  public readonly weekIndex = input.required<number>();

  /**
   * Accessible description of the drawing.
   */
  public readonly label = input('');

  protected readonly viewBox = `0 0 ${PLANET_VIEW_SIZE} ${PLANET_VIEW_SIZE}`;

  private readonly planet = viewChild.required<ElementRef<SVGSVGElement>>('planet');

  constructor() {
    afterRenderEffect(() => {
      this.draw(this.planet().nativeElement, this.weekIndex(), this.guardianLeft());
    });
  }

  /**
   * Rebuilds the whole drawing: the planet, the wounded marks, then the ring.
   */
  private draw(svg: SVGSVGElement, weekIndex: number, guardianLeft: number): void {
    const rn = createSeededRandom(PLANET_SEED);
    const frag = document.createDocumentFragment();
    // A ringed planet keeps room for its rings; a bare globe grows up close to the guardian's ring.
    const side = isRingedPlanet(weekIndex) ? RINGED_PLANET_ART_SIDE : PLANET_ART_SIDE;

    frag.append(
      el('image', {
        href: resolvePlanetArtUrl(weekIndex),
        x: PLANET_CX - side / 2,
        y: PLANET_CY - side / 2,
        width: side,
        height: side,
      }),
    );
    frag.append(this.buildWoundedMarks(rn, side * PLANET_ART_DISC_RATIO));
    frag.append(this.buildRing(guardianLeft));

    svg.replaceChildren(frag);
  }

  /**
   * The wounded: marks laid on the lit face, breathing. They do not count the wounded one by one
   * — the figure is written beside — they say there are people there.
   */
  private buildWoundedMarks(rn: () => number, discRadius: number): SVGGElement {
    const marks = el('g');
    const litX = PLANET_CX + PLANET_ART_LIT_FACE.offset * discRadius;
    const litY = PLANET_CY + PLANET_ART_LIT_FACE.offset * discRadius;
    const litRadius = PLANET_ART_LIT_FACE.radius * discRadius - 8;
    for (let i = 0; i < WOUNDED_MARKS; i++) {
      const a = rn() * Math.PI * 2;
      const d = Math.sqrt(rn()) * discRadius * 0.82;
      const px = PLANET_CX + Math.cos(a) * d;
      const py = PLANET_CY + Math.sin(a) * d * 0.9;
      const period = (2.4 + rn() * 2.6).toFixed(1);
      if (Math.hypot(px - litX, py - litY) > litRadius) {
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
          dur: `${period}s`,
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
   * the others stay in place, extinguished. The same value as the bar under the planet, said in
   * an image.
   */
  private buildRing(guardianLeft: number): SVGGElement {
    const ring = el('g');
    const held = Math.round(RING_SEGMENTS * Math.max(0, Math.min(1, guardianLeft)));
    for (let i = 0; i < RING_SEGMENTS; i++) {
      const angle = (-90 + (i * 360) / RING_SEGMENTS) * (Math.PI / 180);
      const alive = i < held;
      // Extinguished segments stay on the same circle, centred in the standing ones' band.
      const r0 = RING_INNER_RADIUS + (alive ? 0 : 3.5);
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
