import { afterNextRender, DestroyRef, Directive, ElementRef, inject } from '@angular/core';

const NS = 'http://www.w3.org/2000/svg';

/**
 * Point aimed at, in the planet drawing's own coordinates, and the report row it is wired to.
 */
interface Mark {
  readonly vx: number;
  readonly vy: number;
  readonly card: string;
  readonly tone: string;
}

/**
 * The guardian, the wounded on the ground, the ship coming in.
 */
const MARKS: readonly Mark[] = [
  { vx: 292, vy: 106, card: 'target', tone: '#e0404e' },
  { vx: 215, vy: 150, card: 'ground', tone: '#d9954a' },
  { vx: 298, vy: 272, card: 'extraction', tone: '#2dd4bf' },
];

/**
 * Callout wires from the planet to the situation report: a marker on what it points at, a bent
 * line, a foot against the row's inner edge.
 *
 * Ends are measured on the real positions of the rows after layout rather than guessed: a wire
 * that does not reach what it points at points at nothing, and the rows change height with their
 * content. Below the breakpoint the rows sit under the planet and the wires lose their meaning,
 * so nothing is drawn.
 *
 * The host is the scan block; it must hold an `<svg data-wires>`, an element with `data-planet`
 * and one element per mark with `data-card="<name>"`.
 */
@Directive({
  selector: '[appScanWires]',
})
export class ScanWires {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  private observer: ResizeObserver | null = null;

  constructor() {
    afterNextRender(() => {
      const draw = (): void => this.draw();
      draw();
      this.observer = new ResizeObserver(draw);
      this.observer.observe(this.host.nativeElement);
      void document.fonts.ready.then(draw);
    });
    inject(DestroyRef).onDestroy(() => this.observer?.disconnect());
  }

  private draw(): void {
    const scan: HTMLElement = this.host.nativeElement;
    const wires = scan.querySelector<SVGSVGElement>('svg[data-wires]');
    const planet = scan.querySelector<HTMLElement>('[data-planet]');
    if (!wires || !planet) {
      return;
    }

    wires.replaceChildren();
    if (!matchMedia('(width >= 64rem)').matches) {
      return;
    }

    const box = scan.getBoundingClientRect();
    const pb = planet.getBoundingClientRect();
    if (!box.width || !pb.width) {
      return;
    }

    wires.setAttribute('viewBox', `0 0 ${box.width} ${box.height}`);
    const still = matchMedia('(prefers-reduced-motion: reduce)').matches;

    MARKS.forEach((mark, index) => {
      const card = scan.querySelector<HTMLElement>(`[data-card="${mark.card}"]`);
      if (!card) {
        return;
      }
      const cb = card.getBoundingClientRect();

      const x0 = pb.left - box.left + (mark.vx / 360) * pb.width;
      const y0 = pb.top - box.top + (mark.vy / 360) * pb.height;
      const x2 = cb.left - box.left - 10;
      const y2 = cb.top - box.top + Math.min(34, cb.height / 2);

      // A straight stub, a 45° diagonal that makes up the height, then the arrival line. The
      // diagonal is trimmed when the run is short rather than leaving the frame.
      const run = x2 - x0;
      if (run < 30) {
        return;
      }
      const stub = 18;
      const diag = Math.min(Math.abs(y2 - y0), Math.max(0, run - stub - 12));
      const xa = x0 + stub;
      const d = `M${x0} ${y0} H${xa} L${xa + diag} ${y2} H${x2}`;

      wires.append(
        this.node('path', { d, fill: 'none', stroke: '#040a11', 'stroke-width': 4, opacity: 0.6 }),
      );
      const line = this.node('path', {
        d,
        fill: 'none',
        stroke: mark.tone,
        'stroke-width': 1.25,
        opacity: 0.8,
      });
      wires.append(line);

      // The marker: a ring open on the target, and its dot. Two strokes, not a full reticle — the
      // planet is already ringed by its lines.
      wires.append(
        this.node('circle', {
          cx: x0,
          cy: y0,
          r: 5,
          fill: 'none',
          stroke: mark.tone,
          'stroke-width': 1.25,
          opacity: 0.85,
        }),
      );
      wires.append(this.node('circle', { cx: x0, cy: y0, r: 1.6, fill: mark.tone }));
      wires.append(
        this.node('line', {
          x1: x2,
          y1: y2 - 7,
          x2,
          y2: y2 + 7,
          stroke: mark.tone,
          'stroke-width': 1.25,
          opacity: 0.85,
        }),
      );

      if (!still) {
        const length = line.getTotalLength();
        line.style.strokeDasharray = String(length);
        line.style.strokeDashoffset = String(length);
        line.style.animation = `scan-wire 620ms cubic-bezier(0.25, 1, 0.5, 1) ${360 + index * 190}ms forwards`;
      }
    });
  }

  private node<K extends keyof SVGElementTagNameMap>(
    name: K,
    attrs: Readonly<Record<string, string | number>>,
  ): SVGElementTagNameMap[K] {
    const element = document.createElementNS(NS, name);
    for (const [key, value] of Object.entries(attrs)) {
      element.setAttribute(key, String(value));
    }
    return element;
  }
}
