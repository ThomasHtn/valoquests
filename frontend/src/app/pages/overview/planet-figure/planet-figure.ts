import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  viewChild,
} from '@angular/core';

const NS = 'http://www.w3.org/2000/svg';
const CX = 180;
const CY = 180;
const R = 104;
const WOUNDED_MARKS = 26;
const SEGMENTS = 26;
const BRAND = '#d9954a';
const WARM = '#ffc477';
const WARM_CORE = '#fff0cf';
const CYAN = '#2dd4bf';

type Attrs = Readonly<Record<string, string | number>>;

function el<K extends keyof SVGElementTagNameMap>(
  name: K,
  attrs: Attrs = {},
): SVGElementTagNameMap[K] {
  const node = document.createElementNS(NS, name);
  for (const [key, value] of Object.entries(attrs)) {
    node.setAttribute(key, String(value));
  }
  return node;
}

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
  template: `
    <svg
      #planet
      [attr.aria-label]="label()"
      class="block w-full"
      role="img"
      viewBox="0 0 360 360"
    ></svg>
  `,
  styles: `
    :host {
      display: block;
    }
  `,
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

  private readonly planet = viewChild.required<ElementRef<SVGSVGElement>>('planet');

  constructor() {
    afterRenderEffect(() => {
      this.draw(this.planet().nativeElement, this.guardianLeft());
    });
  }

  private draw(svg: SVGSVGElement, guardianLeft: number): void {
    let seed = 815239;
    const rn = (): number => {
      seed = (seed * 1664525 + 1013904223) % 4294967296;
      return seed / 4294967296;
    };

    const frag = document.createDocumentFragment();
    const add = <T extends Node>(node: T): T => {
      frag.appendChild(node);
      return node;
    };
    const defs = add(el('defs'));

    // The globe, lit from the top left: without a terminator, a flat disc does not read as a
    // planet.
    const globe = el('radialGradient', { id: 'planet-globe', cx: 0.34, cy: 0.28, r: 0.85 });
    globe.append(
      el('stop', { offset: 0, 'stop-color': '#6a5a48' }),
      el('stop', { offset: 0.45, 'stop-color': '#3d3629' }),
      el('stop', { offset: 0.8, 'stop-color': '#1b1e1f' }),
      el('stop', { offset: 1, 'stop-color': '#0a1016' }),
    );
    const halo = el('radialGradient', { id: 'planet-halo', cx: 0.5, cy: 0.5, r: 0.5 });
    halo.append(
      el('stop', { offset: 0.62, 'stop-color': BRAND, 'stop-opacity': 0 }),
      el('stop', { offset: 0.78, 'stop-color': BRAND, 'stop-opacity': 0.14 }),
      el('stop', { offset: 1, 'stop-color': BRAND, 'stop-opacity': 0 }),
    );
    const clip = el('clipPath', { id: 'planet-disc' });
    clip.append(el('circle', { cx: CX, cy: CY, r: R }));
    defs.append(globe, halo, clip);

    add(el('circle', { cx: CX, cy: CY, r: R * 1.62, fill: 'url(#planet-halo)' }));
    add(el('circle', { cx: CX, cy: CY, r: R, fill: 'url(#planet-globe)' }));

    // Relief: dark patches cut to the disc. A plain planet looks like a marble.
    const crust = el('g', { 'clip-path': 'url(#planet-disc)' });
    for (let i = 0; i < 11; i++) {
      const a = rn() * Math.PI * 2;
      const d = Math.sqrt(rn()) * R * 0.88;
      crust.append(
        el('ellipse', {
          cx: (CX + Math.cos(a) * d).toFixed(1),
          cy: (CY + Math.sin(a) * d * 0.82).toFixed(1),
          rx: (10 + rn() * 26).toFixed(1),
          ry: (6 + rn() * 13).toFixed(1),
          fill: rn() < 0.55 ? '#000000' : '#8a755a',
          opacity: (0.1 + rn() * 0.13).toFixed(2),
          transform: `rotate(${(rn() * 60 - 30).toFixed(0)} ${CX} ${CY})`,
        }),
      );
    }
    add(crust);

    // The atmosphere, and the shadow taking the right edge.
    add(
      el('circle', {
        cx: CX + 26,
        cy: CY + 20,
        r: R,
        fill: '#040a11',
        opacity: 0.42,
        'clip-path': 'url(#planet-disc)',
      }),
    );
    add(
      el('circle', {
        cx: CX,
        cy: CY,
        r: R + 1,
        fill: 'none',
        stroke: WARM,
        'stroke-width': 1,
        opacity: 0.28,
      }),
    );

    // The wounded: marks laid on the lit face, breathing. They do not count the wounded one by
    // one — the figure is written beside — they say there are people there.
    const marks = el('g');
    for (let i = 0; i < WOUNDED_MARKS; i++) {
      const a = rn() * Math.PI * 2;
      const d = Math.sqrt(rn()) * R * 0.74;
      const px = CX + Math.cos(a) * d;
      const py = CY + Math.sin(a) * d * 0.9;
      if (px - CX > 34 && py - CY > 24) {
        continue; // nothing in the shadow
      }
      const mark = el('circle', { cx: px.toFixed(1), cy: py.toFixed(1), r: 2.6, fill: WARM_CORE });
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
        el('circle', { cx: px.toFixed(1), cy: py.toFixed(1), r: 7, fill: WARM, opacity: 0.1 }),
      );
    }
    add(marks);

    // The breakthrough. One segment per twenty-sixth of the hit points: those left are standing,
    // in red; the others drift outward, extinguished. The same value as the bar under the planet,
    // said in an image.
    const ring = el('g');
    const held = Math.round(SEGMENTS * Math.max(0, Math.min(1, guardianLeft)));
    for (let i = 0; i < SEGMENTS; i++) {
      const angle = (-90 + (i * 360) / SEGMENTS) * (Math.PI / 180);
      const alive = i < held;
      const drift = alive ? 0 : 9 + ((i * 37) % 11);
      const r0 = R + 20 + drift;
      const r1 = r0 + (alive ? 13 : 6);
      const segment = el('line', {
        x1: (CX + Math.cos(angle) * r0).toFixed(1),
        y1: (CY + Math.sin(angle) * r0).toFixed(1),
        x2: (CX + Math.cos(angle) * r1).toFixed(1),
        y2: (CY + Math.sin(angle) * r1).toFixed(1),
        stroke: alive ? '#e0404e' : '#4a5560',
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
    add(ring);

    // The ship's approach, from the bottom right. It passes beside the globe and never in front:
    // that side is also where the three callout wires leave from.
    add(
      el('path', {
        d: `M356 356 Q332 320 ${CX + 118} ${CY + 92}`,
        fill: 'none',
        stroke: CYAN,
        'stroke-width': 1.4,
        'stroke-dasharray': '5 7',
        opacity: 0.4,
      }),
    );
    add(
      el('path', {
        d: 'M0 -8 L7 8 L0 5 L-7 8 Z',
        fill: CYAN,
        opacity: 0.9,
        transform: `translate(${CX + 118} ${CY + 92}) rotate(-42)`,
      }),
    );

    svg.replaceChildren(frag);
  }
}
