import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  input,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { svgElement as el } from '@shared/rocket/rocket-drawing';
import { BoardRow } from '../leaderboard.model';

const WIDTH = 1600;
const HEIGHT = 280;
const STAR_COUNT = 140;
const STAR = '#ece8e1';
const EMBER = '#e8ab6b';

/**
 * The week's three leaders on their plinths, under the same sky as the campaign's road. Each
 * stands under the same round portrait the board uses, larger for 1st.
 *
 * Stars only, on the page's own ground: the podium is a block of the board's page, not a scene,
 * so it takes no night plate of its own. The plinths keep their heights from the first podium —
 * 1st a step above 2nd, 2nd a step above 3rd — and the ground rule under them is the board's top
 * edge, which is why this component draws none of its own.
 */
@Component({
  selector: 'app-podium',
  imports: [RouterLink, TranslatePipe, Avatar, ChampionBadge],
  templateUrl: './podium.html',
  styleUrl: './podium.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
})
export class Podium {
  /**
   * The ranked rows, in order; only the first three are drawn.
   */
  public readonly rows = input.required<readonly BoardRow[]>();

  protected readonly top = computed(() => this.rows().slice(0, 3));

  private readonly sky = viewChild.required<ElementRef<SVGSVGElement>>('sky');

  constructor() {
    afterNextRender(() => this.draw(this.sky().nativeElement));
  }

  /**
   * The same seeded field as the campaign's sky, thinned and settling toward the horizon: the
   * squared draw on `cy` piles most stars low, where the plinths stand, and leaves the top sparse.
   */
  private draw(svg: SVGSVGElement): void {
    let seed = 20260905;
    const random = (): number => {
      seed = (seed * 1664525 + 1013904223) % 4294967296;
      return seed / 4294967296;
    };
    const frag = document.createDocumentFragment();
    for (let i = 0; i < STAR_COUNT; i++) {
      const y = random();
      frag.append(
        el('circle', {
          cx: (random() * WIDTH).toFixed(1),
          cy: (y * y * (HEIGHT - 30)).toFixed(1),
          r: (0.5 + random() * 1.1).toFixed(2),
          fill: random() < 0.15 ? EMBER : STAR,
          opacity: (0.15 + random() * 0.55).toFixed(2),
        }),
      );
    }
    svg.replaceChildren(frag);
  }
}
