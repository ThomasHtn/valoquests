import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucidePlay } from '@lucide/angular';

import { resolveBossCategoryColorClass } from '@core/boss/boss-visual.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { PageHeader } from '@layout/page-header/page-header';
import { RuleSection } from './rule-section/rule-section';
import {
  BOSS_CATEGORY_SHOWCASE,
  DIFFICULTY_SHOWCASE,
  MATCH_DAMAGE_SHOWCASE,
  RANKING_FACTS,
  REGULARITY_BONUS_SHOWCASE,
  TEAM_BONUS_SHOWCASE,
} from './rules.constants';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * Rules page.
 *
 * Static walkthrough of the weekly loop, read top to bottom: one section per beat, each pairing a
 * short narrative with the numbers behind it, for anyone landing on the tracker without prior
 * context.
 */
@Component({
  selector: 'app-rules',
  imports: [TranslatePipe, RouterLink, ChampionBadge, LucidePlay, PageHeader, RuleSection],
  templateUrl: './rules.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Rules {
  /**
   * Damage ladder shown in the challenges panel, exposed to the template.
   */
  protected readonly difficultyShowcase = DIFFICULTY_SHOWCASE;

  /**
   * Per-mode match damage ladder shown in the damage panel, exposed to the template.
   */
  protected readonly matchDamageShowcase = MATCH_DAMAGE_SHOWCASE;

  /**
   * Regularity bonus ladder shown in the bonuses panel, exposed to the template.
   */
  protected readonly regularityBonusShowcase = REGULARITY_BONUS_SHOWCASE;

  /**
   * Team bonus ladder shown in the bonuses panel, exposed to the template.
   */
  protected readonly teamBonusShowcase = TEAM_BONUS_SHOWCASE;

  /**
   * Boss weight classes shown in the boss panel, exposed to the template.
   */
  protected readonly bossCategoryShowcase = BOSS_CATEGORY_SHOWCASE;

  /**
   * Facts closing the ranking panel, exposed to the template.
   */
  protected readonly rankingFacts = RANKING_FACTS;

  /**
   * Resolves a boss category's text color utility, exposed to the template.
   */
  protected readonly bossCategoryColorClass = resolveBossCategoryColorClass;
}
