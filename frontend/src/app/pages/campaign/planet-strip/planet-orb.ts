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
import { PlanetState } from '../campaign.model';
import { nextInstanceId } from '@core/dom/instance-id.utils';
import {
  AHEAD_FILL,
  AHEAD_STROKE,
  ORB_CX as CX,
  ORB_CY as CY,
  ORB_TONES,
  ORB_VIEW_SIZE,
  RING_PLATE,
  SEA_COUNT,
} from './planet-orb.constants';

/**
 * One planet of the strip: its ground, a few dark seas, its shading, and the ring of the
 * breakthrough around it. A planet still ahead is a dotted disc: a place, not a world yet.
 */
@Component({
  selector: 'app-planet-orb',
  templateUrl: './planet-orb.html',
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

  protected readonly viewBox = `0 0 ${ORB_VIEW_SIZE} ${ORB_VIEW_SIZE}`;

  private readonly orb = viewChild.required<ElementRef<SVGSVGElement>>('orb');

  /**

   * Unique gradient id: several orbs share the page, and gradient ids are global to it.

   */
  private readonly id = nextInstanceId('orb');

  constructor() {
    afterRenderEffect(() => this.draw(this.orb().nativeElement));
  }

  private draw(svg: SVGSVGElement): void {
    const r = this.radius();
    const frag = document.createDocumentFragment();

    if (this.state() === 'ahead') {
      frag.append(this.buildAheadDisc(r));
    } else {
      frag.append(this.buildShadeDefs());
      frag.append(el('circle', { cx: CX, cy: CY, r, fill: this.hue() }));
      frag.append(...this.buildSeas(r));
      frag.append(el('circle', { cx: CX, cy: CY, r, fill: `url(#${this.id})` }));
      frag.append(...this.buildRing(r));
    }
    svg.replaceChildren(frag);
  }

  /**
   * Dotted disc of a planet not reached yet.
   */
  private buildAheadDisc(r: number): SVGCircleElement {
    return el('circle', {
      cx: CX,
      cy: CY,
      r,
      fill: AHEAD_FILL,
      stroke: AHEAD_STROKE,
      'stroke-width': 1,
      'stroke-dasharray': '3 3',
    });
  }

  /**
   * Radial shading: a highlight top left, a shadow on the far edge.
   */
  private buildShadeDefs(): SVGDefsElement {
    const defs = el('defs');
    const shade = el('radialGradient', { id: this.id, cx: 0.32, cy: 0.3, r: 0.8 });
    shade.append(
      el('stop', { offset: 0, 'stop-color': '#ffffff', 'stop-opacity': 0.22 }),
      el('stop', { offset: 0.55, 'stop-color': '#ffffff', 'stop-opacity': 0 }),
      el('stop', { offset: 1, 'stop-color': '#000000', 'stop-opacity': 0.65 }),
    );
    defs.append(shade);
    return defs;
  }

  /**
   * A few dark ellipses placed from the seed.
   */
  private buildSeas(r: number): readonly SVGEllipseElement[] {
    const random = createSeededRandom(this.seed() * 977 + 13);
    const seas: SVGEllipseElement[] = [];
    for (let k = 0; k < SEA_COUNT; k++) {
      const angle = random() * Math.PI * 2;
      const distance = Math.sqrt(random()) * r * 0.8;
      seas.push(
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
    return seas;
  }

  /**
   * The ring: the guardian's lines, eaten from the top clockwise.
   */
  private buildRing(r: number): readonly SVGCircleElement[] {
    const ringRadius = r + Math.max(6, r * 0.3);
    const length = 2 * Math.PI * ringRadius;
    const plate = el('circle', {
      cx: CX,
      cy: CY,
      r: ringRadius,
      fill: 'none',
      stroke: RING_PLATE,
      'stroke-width': 3,
    });
    const arc = el('circle', {
      cx: CX,
      cy: CY,
      r: ringRadius,
      fill: 'none',
      stroke: ORB_TONES[this.state()],
      'stroke-width': 3,
      'stroke-dasharray': `${(length * this.advance()).toFixed(1)} ${length.toFixed(1)}`,
      transform: `rotate(-90 ${CX} ${CY})`,
    });
    return [plate, arc];
  }
}
