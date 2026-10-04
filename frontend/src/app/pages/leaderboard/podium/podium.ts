import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  input,
  viewChild,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { createSeededRandom } from '@core/random/seeded-random.utils';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { TitleBadge } from '@shared/title-badge/title-badge';
import { svgElement as el } from '@core/svg/svg-element.utils';
import { BoardRow } from '../leaderboard.model';
import { groupPodium } from './podium.utils';
import { WIDTH, HEIGHT, STAR_COUNT, STAR, EMBER, SKY_SEED } from './podium.constants';

/**
 * The week's top three on plinths under a star field.
 * Draws no ground rule: the board's top edge serves as one.
 */
@Component({
  selector: 'app-podium',
  imports: [NgTemplateOutlet, RouterLink, TranslatePipe, Avatar, ChampionBadge, TitleBadge],
  templateUrl: './podium.html',
  styleUrl: './podium.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
})
export class Podium {
  /**
   * Ranked rows in order; only places one to three are drawn, ties included.
   */
  public readonly rows = input.required<readonly BoardRow[]>();

  /**
   * Occupied plinths, ties sharing theirs.
   */
  protected readonly places = computed(() => groupPodium(this.rows()));

  /**
   * Star field canvas, filled once after the first render.
   */
  private readonly sky = viewChild.required<ElementRef<SVGSVGElement>>('sky');

  constructor() {
    afterNextRender(() => this.draw(this.sky().nativeElement));
  }

  /**
   * Seeded star field; squaring `cy` piles stars low near the plinths.
   */
  private draw(svg: SVGSVGElement): void {
    const random = createSeededRandom(SKY_SEED);
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
