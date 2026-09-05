import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  linkedSignal,
  viewChild,
} from '@angular/core';
import { LucideCheck, LucideLock, LucideTarget } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import {
  drawShip,
  noseHeight,
  outline,
  ROCKET_PART_COUNT,
  SHIP,
  SKIRT,
  svgElement as el,
} from '@shared/rocket/rocket-drawing';
import { RocketPart } from '../campaign.model';

const VIEW_WIDTH = 600;
const VIEW_HEIGHT = 520;
const BASE_Y = 468;
const CENTER_X = 300;
const BLUE = '#7fb6d8';
const AMBER = '#e8ab6b';
const DISPLAY_FONT = 'font-family: var(--font-display)';
const MONO_FONT = 'font-family: var(--font-mono)';

/**
 * The rocket on its blueprint, part by part.
 *
 * The finished launcher stays as a dotted outline; the ship built so far is drawn over it at the
 * stage chosen in the parts list. A fitted part can be selected to see the ship as it was then;
 * a coming part waits for the guardian it asks for.
 */
@Component({
  selector: 'app-rocket-showcase',
  imports: [TranslatePipe, LucideCheck, LucideLock, LucideTarget],
  templateUrl: './rocket-showcase.html',
  styleUrl: './rocket-showcase.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RocketShowcase {
  public readonly parts = input.required<readonly RocketPart[]>();
  public readonly campaignNumber = input.required<number>();

  private readonly translation = inject(Translation);
  private readonly viewer = viewChild.required<ElementRef<SVGSVGElement>>('viewer');

  protected readonly total = ROCKET_PART_COUNT;

  protected readonly builtCount = computed(
    () => this.parts().filter((part) => part.state === 'built').length,
  );

  /**
   * Stage on display: the latest fitted part until one is chosen, and back to it on new data.
   */
  protected readonly shown = linkedSignal(() => this.builtCount());

  protected readonly shownPart = computed<RocketPart | null>(() => {
    const index = this.shown();
    return this.parts().find((part) => part.index === index) ?? null;
  });

  constructor() {
    afterRenderEffect(() => {
      this.render(this.viewer().nativeElement, this.shown());
    });
  }

  protected select(part: RocketPart): void {
    if (part.state === 'built') {
      this.shown.set(part.index);
    }
  }

  private render(svg: SVGSVGElement, stage: number): void {
    svg.replaceChildren();
    const frame = document.createDocumentFragment();
    const put = (node: SVGElement): void => {
      frame.appendChild(node);
    };

    put(el('rect', { x: 0, y: 0, width: VIEW_WIDTH, height: VIEW_HEIGHT, fill: '#0c1b28' }));
    for (let x = 0; x <= VIEW_WIDTH; x += 25) {
      put(this.gridLine(x, x, 0, VIEW_HEIGHT, x % 100 === 0));
    }
    for (let y = 0; y <= VIEW_HEIGHT; y += 25) {
      put(this.gridLine(0, VIEW_WIDTH, y, y, y % 100 === 0));
    }
    const corners: readonly [number, number][] = [
      [14, 14],
      [VIEW_WIDTH - 14, 14],
      [14, VIEW_HEIGHT - 14],
      [VIEW_WIDTH - 14, VIEW_HEIGHT - 14],
    ];
    for (const [x, y] of corners) {
      put(
        el('path', {
          d: `M${x - 8} ${y} H${x + 8} M${x} ${y - 8} V${y + 8}`,
          stroke: 'rgb(127 182 216 / 45%)',
          'stroke-width': 1,
        }),
      );
    }

    // Ground line with its hatches.
    put(this.stroke(120, VIEW_WIDTH - 120, BASE_Y, BASE_Y, 'rgb(127 182 216 / 55%)', 1.5));
    for (let x = 124; x < VIEW_WIDTH - 120; x += 12) {
      put(this.stroke(x, x - 8, BASE_Y, BASE_Y + 8, 'rgb(127 182 216 / 35%)', 1));
    }

    // The finished launcher as a template, then the ship built so far over it.
    const upright = `translate(${CENTER_X} ${BASE_Y}) scale(1 -1)`;
    const ghost = el('g', { transform: upright });
    ghost.appendChild(
      el('path', {
        d: outline(SHIP[ROCKET_PART_COUNT]),
        fill: 'none',
        stroke: BLUE,
        'stroke-width': 1.4,
        'stroke-dasharray': '5 6',
        opacity: 0.55,
      }),
    );
    put(ghost);
    const built = el('g', { transform: upright });
    built.appendChild(drawShip(stage));
    put(built);

    this.drawDimension(put, stage);
    this.drawCartouche(put);
    svg.appendChild(frame);
  }

  /**
   * Height dimension: what stands, measured against the finished launcher.
   */
  private drawDimension(put: (node: SVGElement) => void, stage: number): void {
    const full = SHIP[ROCKET_PART_COUNT];
    const fullTop = BASE_Y - (SKIRT + full.h + noseHeight(full) + full.w * 2.2);
    const now = SHIP[stage];
    const capsule = now.nose === 'capsule' ? now.w * 2.2 : 0;
    const nowTop = stage ? BASE_Y - (SKIRT + now.h + noseHeight(now) + capsule) : BASE_Y;
    const x = 96;
    put(
      el('line', {
        x1: x,
        x2: x,
        y1: fullTop,
        y2: BASE_Y,
        stroke: 'rgb(127 182 216 / 40%)',
        'stroke-width': 1,
        'stroke-dasharray': '3 4',
      }),
    );
    put(this.stroke(x, x, nowTop, BASE_Y, AMBER, 1.5));
    for (const y of [nowTop, BASE_Y]) {
      put(this.stroke(x - 6, x + 6, y, y, AMBER, 1.5));
    }
    put(this.stroke(x - 6, x + 6, fullTop, fullTop, 'rgb(127 182 216 / 60%)', 1));
    put(
      this.text(x - 12, nowTop + 4, `${stage} / ${ROCKET_PART_COUNT}`, {
        'text-anchor': 'end',
        fill: AMBER,
        'font-size': 14,
        'font-weight': 600,
        'letter-spacing': 1,
        style: DISPLAY_FONT,
      }),
    );
  }

  /**
   * The plan's cartouche, bottom right.
   */
  private drawCartouche(put: (node: SVGElement) => void): void {
    const x = VIEW_WIDTH - 214;
    const y = VIEW_HEIGHT - 70;
    put(
      el('rect', {
        x,
        y,
        width: 200,
        height: 56,
        fill: 'rgb(12 27 40 / 85%)',
        stroke: 'rgb(127 182 216 / 45%)',
        'stroke-width': 1,
      }),
    );
    put(this.stroke(x, x + 200, y + 20, y + 20, 'rgb(127 182 216 / 35%)', 1));
    const mono = { fill: '#868b8d', 'font-size': 9, 'letter-spacing': 2, style: MONO_FONT };
    const t = (key: string, params?: Record<string, string | number>): string =>
      this.translation.translate(`campaign.rocket.${key}`, params).toUpperCase();
    put(this.text(x + 10, y + 14, t('cartoucheTitle', { number: this.campaignNumber() }), mono));
    const part = this.shownPart();
    const line = part
      ? t('cartouchePart', { name: part.name, index: part.label })
      : t('cartoucheEmpty');
    put(
      this.text(x + 10, y + 36, line, {
        fill: '#ece8e1',
        'font-size': 13,
        'font-weight': 600,
        'letter-spacing': 1,
        style: DISPLAY_FONT,
      }),
    );
    const week = part?.week ?? null;
    if (week !== null) {
      put(this.text(x + 10, y + 49, t('cartoucheWeek', { week }), mono));
    }
  }

  private gridLine(x1: number, x2: number, y1: number, y2: number, major: boolean): SVGElement {
    const stroke = major ? 'rgb(127 182 216 / 13%)' : 'rgb(127 182 216 / 6%)';
    return this.stroke(x1, x2, y1, y2, stroke, 1);
  }

  private stroke(
    x1: number,
    x2: number,
    y1: number,
    y2: number,
    stroke: string,
    width: number,
  ): SVGElement {
    return el('line', { x1, x2, y1, y2, stroke, 'stroke-width': width });
  }

  private text(
    x: number,
    y: number,
    content: string,
    attrs: Readonly<Record<string, string | number>>,
  ): SVGElement {
    const node = el('text', { x, y, ...attrs });
    node.textContent = content;
    return node;
  }
}
