import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  LucideArrowLeft,
  LucideSkull,
  LucideSwords,
  LucideTarget,
  LucideTrophy,
} from '@lucide/angular';

import { resolveBossCategoryColorClass } from '@core/boss/boss-visual.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import {
  BOSS_CATEGORY_SHOWCASE,
  DIFFICULTY_SHOWCASE,
  MATCH_DAMAGE_SHOWCASE,
  REGULARITY_BONUS_SHOWCASE,
  TEAM_BONUS_SHOWCASE,
} from './rules.constants';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * Rules page.
 *
 * Static, storybook-style walkthrough of the weekly loop: challenges, damage, the shared boss
 * fight and the ranking, for anyone landing on the tracker without prior context.
 */
@Component({
  selector: 'app-rules',
  imports: [
    TranslatePipe,
    RouterLink,
    ChampionBadge,
    LucideArrowLeft,
    LucideTarget,
    LucideSwords,
    LucideSkull,
    LucideTrophy,
  ],
  templateUrl: './rules.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Rules {
  /**
   * Damage ladder shown in the challenges step, exposed to the template.
   */
  protected readonly difficultyShowcase = DIFFICULTY_SHOWCASE;

  /**
   * Per-mode match damage ladder shown in the damage step, exposed to the template.
   */
  protected readonly matchDamageShowcase = MATCH_DAMAGE_SHOWCASE;

  /**
   * Regularity bonus ladder shown in the damage step, exposed to the template.
   */
  protected readonly regularityBonusShowcase = REGULARITY_BONUS_SHOWCASE;

  /**
   * Team bonus ladder shown in the damage step, exposed to the template.
   */
  protected readonly teamBonusShowcase = TEAM_BONUS_SHOWCASE;

  /**
   * Boss weight classes shown in the boss step, exposed to the template.
   */
  protected readonly bossCategoryShowcase = BOSS_CATEGORY_SHOWCASE;

  /**
   * Resolves a boss category's text color utility, exposed to the template.
   */
  protected readonly bossCategoryColorClass = resolveBossCategoryColorClass;
}
