import { afterNextRender, DestroyRef, Directive, ElementRef, inject } from '@angular/core';

import { svgElement } from '@core/svg/svg-element.utils';

import { PLANET_VIEW_SIZE } from '../planet-figure/planet-figure.constants';
import { MARKS, WIRE_HALO } from './scan-wires.constants';

/**
 * Callout wires from the planet to the report rows, measured after layout; none below 64rem.
 * The host must hold an `<svg data-wires>`, a `data-planet` element and one `data-card` per mark.
 */
@Directive({
  selector: '[appScanWires]',
})
export class ScanWires {
  /**
   * Host element holding the wires layer, the planet and the cards.
   */
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  /**
   * Redraws the wires when the host or its children resize.
   */
  private observer: ResizeObserver | null = null;

  constructor() {
    afterNextRender(() => {
      const draw = (): void => this.draw();
      draw();
      this.observer = new ResizeObserver(draw);
      // Children are watched too: the report can change height without resizing the host.
      const host = this.host.nativeElement;
      this.observer.observe(host);
      for (const child of Array.from(host.children)) {
        this.observer.observe(child);
      }
      void document.fonts.ready.then(draw);
    });
    inject(DestroyRef).onDestroy(() => this.observer?.disconnect());
  }

  /**
   * Redraws every wire from the current layout, or clears them below 64rem.
   */
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
    const planetBox = planet.getBoundingClientRect();
    if (!box.width || !planetBox.width) {
      return;
    }

    wires.setAttribute('viewBox', `0 0 ${box.width} ${box.height}`);
    const still = matchMedia('(prefers-reduced-motion: reduce)').matches;

    MARKS.forEach((mark, index) => {
      const card = scan.querySelector<HTMLElement>(`[data-card="${mark.card}"]`);
      if (!card) {
        return;
      }
      const cardBox = card.getBoundingClientRect();

      const x0 = planetBox.left - box.left + (mark.vx / PLANET_VIEW_SIZE) * planetBox.width;
      const y0 = planetBox.top - box.top + (mark.vy / PLANET_VIEW_SIZE) * planetBox.height;
      const x2 = cardBox.left - box.left - 10;
      // Lands on the row's vertical centre.
      const y2 = cardBox.top - box.top + cardBox.height / 2;

      // Stub, 45° diagonal, arrival line; the diagonal is trimmed on a short run.
      const run = x2 - x0;
      if (run < 30) {
        return;
      }
      const stub = 18;
      const diag = Math.min(Math.abs(y2 - y0), Math.max(0, run - stub - 12));
      const xa = x0 + stub;
      const d = `M${x0} ${y0} H${xa} L${xa + diag} ${y2} H${x2}`;

      wires.append(
        svgElement('path', { d, fill: 'none', stroke: WIRE_HALO, 'stroke-width': 4, opacity: 0.6 }),
      );
      const line = svgElement('path', {
        d,
        fill: 'none',
        stroke: mark.tone,
        'stroke-width': 1.25,
        opacity: 0.8,
      });
      wires.append(line);

      // Marker: a ring and its dot, not a full reticle (the planet is already ringed).
      wires.append(
        svgElement('circle', {
          cx: x0,
          cy: y0,
          r: 5,
          fill: 'none',
          stroke: mark.tone,
          'stroke-width': 1.25,
          opacity: 0.85,
        }),
      );
      wires.append(svgElement('circle', { cx: x0, cy: y0, r: 1.6, fill: mark.tone }));
      wires.append(
        svgElement('line', {
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
}
