import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  viewChild,
} from '@angular/core';

import { PLANET_ART_DISC_RATIO } from '@core/campaign/planet-art.constants';
import { resolvePlanetArtUrl } from '@core/campaign/planet-art.utils';
import { svgElement as el } from '@core/svg/svg-element.utils';
import { PlanetState } from '../campaign.model';
import {
  AHEAD_ART_STYLE,
  ORB_CX as CX,
  ORB_CY as CY,
  ORB_TONES,
  ORB_VIEW_SIZE,
  RING_GAP,
  RING_PLATE,
  RING_PLATE_WON,
  RING_SCALE,
} from './planet-orb.constants';

/**
 * One planet of the strip: its drawing, and the ring of the breakthrough around it. A planet still
 * ahead is a grey silhouette without a ring: a place on the map, not a world reached yet.
 */
@Component({
  selector: 'app-planet-orb',
  templateUrl: './planet-orb.html',
  host: { class: 'block size-full' },
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanetOrb {
  /**
   * Radius of the globe, in viewBox units.
   */
  public readonly radius = input.required<number>();

  /**
   * One-based week of the planet, which picks its drawing: the same one as on the overview.
   */
  public readonly weekIndex = input.required<number>();

  public readonly state = input.required<PlanetState>();

  /**
   * Share of the guardian's hit points still standing, in [0, 1].
   */
  public readonly standing = input.required<number>();

  protected readonly viewBox = `0 0 ${ORB_VIEW_SIZE} ${ORB_VIEW_SIZE}`;

  private readonly orb = viewChild.required<ElementRef<SVGSVGElement>>('orb');

  constructor() {
    afterRenderEffect(() => this.draw(this.orb().nativeElement));
  }

  private draw(svg: SVGSVGElement): void {
    const r = this.radius();
    const ahead = this.state() === 'ahead';
    const frag = document.createDocumentFragment();

    frag.append(this.buildArt(r, ahead));
    if (!ahead) {
      frag.append(...this.buildRing(r));
    }
    svg.replaceChildren(frag);
  }

  /**
   * The planet's drawing, scaled so its globe has the requested radius.
   */
  private buildArt(r: number, ahead: boolean): SVGImageElement {
    const side = r / PLANET_ART_DISC_RATIO;
    const art = el('image', {
      href: resolvePlanetArtUrl(this.weekIndex()),
      x: (CX - side / 2).toFixed(1),
      y: (CY - side / 2).toFixed(1),
      width: side.toFixed(1),
      height: side.toFixed(1),
    });
    if (ahead) {
      art.setAttribute('style', AHEAD_ART_STYLE);
    }
    return art;
  }

  /**
   * The ring: the guardian's hit points left, full before the fight and empty once it is down.
   * Laid past the widest planetary ring so the two never cross.
   */
  private buildRing(r: number): readonly SVGCircleElement[] {
    const ringRadius = r * RING_SCALE + RING_GAP;
    const length = 2 * Math.PI * ringRadius;
    const plate = el('circle', {
      cx: CX,
      cy: CY,
      r: ringRadius,
      fill: 'none',
      stroke: this.state() === 'won' ? RING_PLATE_WON : RING_PLATE,
      'stroke-width': 3,
    });
    const arc = el('circle', {
      cx: CX,
      cy: CY,
      r: ringRadius,
      fill: 'none',
      stroke: ORB_TONES[this.state()],
      'stroke-width': 3,
      'stroke-dasharray': `${(length * this.standing()).toFixed(1)} ${length.toFixed(1)}`,
      transform: `rotate(-90 ${CX} ${CY})`,
      // Drains from full on arrival (`.fx-arc-drain`).
      class: 'fx-arc-drain',
      style: `--arc-full: ${length.toFixed(1)}`,
    });
    return [plate, arc];
  }
}
