import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  viewChild,
} from '@angular/core';

import {
  PLANET_ART_DISC_RATIO,
  PLANET_ART_LIT_FACE,
} from '@core/campaign/planets/campaign-planet-art.constants';
import {
  isRingedPlanet,
  resolvePlanetArtUrl,
} from '@core/campaign/planets/campaign-planet-art.utils';
import { createSeededRandom } from '@core/random/seeded-random.utils';
import { svgElement } from '@core/svg/svg-element.utils';
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
 * Planet of the week: wounded marks on its lit face, the guardian's ring around it.
 */
@Component({
  selector: 'app-planet-figure',
  templateUrl: './planet-figure.html',
  styleUrl: './planet-figure.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanetFigure {
  /**
   * Share of the guardian's hit points left, in [0, 1].
   */
  public readonly guardianLeft = input.required<number>();

  /**
   * One-based week, which picks the same drawing as on the frieze.
   */
  public readonly weekIndex = input.required<number>();

  /**
   * Accessible description of the drawing.
   */
  public readonly label = input('');

  /**
   * Square drawing frame the planet and its ring are laid out in.
   */
  protected readonly viewBox = `0 0 ${PLANET_VIEW_SIZE} ${PLANET_VIEW_SIZE}`;

  /**
   * SVG element the planet is drawn into.
   */
  private readonly planet = viewChild.required<ElementRef<SVGSVGElement>>('planet');

  constructor() {
    afterRenderEffect(() => {
      this.draw(this.planet().nativeElement, this.weekIndex(), this.guardianLeft());
    });
  }

  /**
   * Rebuilds the planet, the wounded marks, then the ring.
   */
  private draw(svg: SVGSVGElement, weekIndex: number, guardianLeft: number): void {
    const random = createSeededRandom(PLANET_SEED);
    const fragment = document.createDocumentFragment();
    // A ringed planet keeps room for its rings; a bare globe grows close to the guardian's ring.
    const side = isRingedPlanet(weekIndex) ? RINGED_PLANET_ART_SIDE : PLANET_ART_SIDE;

    fragment.append(
      svgElement('image', {
        href: resolvePlanetArtUrl(weekIndex),
        x: PLANET_CX - side / 2,
        y: PLANET_CY - side / 2,
        width: side,
        height: side,
      }),
    );
    fragment.append(this.buildWoundedMarks(random, side * PLANET_ART_DISC_RATIO));
    fragment.append(this.buildRing(guardianLeft));

    svg.replaceChildren(fragment);
  }

  /**
   * Breathing marks on the lit face, a texture rather than a count.
   */
  private buildWoundedMarks(random: () => number, discRadius: number): SVGGElement {
    const marks = svgElement('g');
    const litX = PLANET_CX + PLANET_ART_LIT_FACE.offset * discRadius;
    const litY = PLANET_CY + PLANET_ART_LIT_FACE.offset * discRadius;
    const litRadius = PLANET_ART_LIT_FACE.radius * discRadius - 8;
    for (let i = 0; i < WOUNDED_MARKS; i++) {
      const angle = random() * Math.PI * 2;
      const distance = Math.sqrt(random()) * discRadius * 0.82;
      const px = PLANET_CX + Math.cos(angle) * distance;
      const py = PLANET_CY + Math.sin(angle) * distance * 0.9;
      // Drawn before the shadow check, so a skipped mark still consumes the seeded sequence.
      const period = (2.4 + random() * 2.6).toFixed(1);
      if (Math.hypot(px - litX, py - litY) > litRadius) {
        // Nothing in the shadow.
        continue;
      }
      const mark = svgElement('circle', {
        cx: px.toFixed(1),
        cy: py.toFixed(1),
        r: 2.6,
        fill: PLANET_COLORS.warmCore,
      });
      mark.append(
        svgElement('animate', {
          attributeName: 'opacity',
          values: '0.95;0.35;0.95',
          dur: `${period}s`,
          repeatCount: 'indefinite',
        }),
      );
      marks.append(mark);
      marks.append(
        svgElement('circle', {
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
   * Breakthrough ring: standing segments in red, the others extinguished in place.
   */
  private buildRing(guardianLeft: number): SVGGElement {
    const ring = svgElement('g');
    const held = Math.round(RING_SEGMENTS * Math.max(0, Math.min(1, guardianLeft)));
    for (let i = 0; i < RING_SEGMENTS; i++) {
      const angle = (-90 + (i * 360) / RING_SEGMENTS) * (Math.PI / 180);
      const alive = i < held;
      // Extinguished segments stay centred in the standing ones' band.
      const r0 = RING_INNER_RADIUS + (alive ? 0 : 3.5);
      const r1 = r0 + (alive ? 13 : 6);
      const segment = svgElement('line', {
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
          svgElement('animate', {
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
