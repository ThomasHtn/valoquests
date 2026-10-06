import { NgTemplateOutlet } from '@angular/common';
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
import { createSeededRandom } from '@core/random/seeded-random.utils';
import { svgElement } from '@core/svg/svg-element.utils';
import { Avatar } from '@shared/avatar/avatar';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { TitleBadge } from '@shared/title-badge/title-badge';

import { BoardRow } from '../leaderboard.model';
import {
  EMBER_COLOR,
  SKY_HEIGHT,
  SKY_SEED,
  SKY_WIDTH,
  STAR_COLOR,
  STAR_COUNT,
} from './podium.constants';
import { groupPodium } from './podium.utils';

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
   * Sky viewBox, the frame the stars are drawn in.
   */
  protected readonly viewBox = `0 0 ${SKY_WIDTH} ${SKY_HEIGHT}`;

  /**
   * Star field canvas, filled once after the first render.
   */
  private readonly sky = viewChild.required<ElementRef<SVGSVGElement>>('sky');

  /**
   * Draws the sky once the canvas exists.
   */
  constructor() {
    afterNextRender(() => this.draw(this.sky().nativeElement));
  }

  /**
   * Seeded star field; squaring `cy` piles stars low near the plinths.
   */
  private draw(svg: SVGSVGElement): void {
    const random = createSeededRandom(SKY_SEED);
    const stars = document.createDocumentFragment();
    for (let i = 0; i < STAR_COUNT; i++) {
      const y = random();
      stars.append(
        svgElement('circle', {
          cx: (random() * SKY_WIDTH).toFixed(1),
          cy: (y * y * (SKY_HEIGHT - 30)).toFixed(1),
          r: (0.5 + random() * 1.1).toFixed(2),
          fill: random() < 0.15 ? EMBER_COLOR : STAR_COLOR,
          opacity: (0.15 + random() * 0.55).toFixed(2),
        }),
      );
    }
    svg.replaceChildren(stars);
  }
}
