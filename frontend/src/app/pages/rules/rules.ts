import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucidePlay } from '@lucide/angular';

import { resolveBossCategoryColorClass } from '@core/boss/boss-visual.utils';
import { formatSquadMultiplier } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { PageHeader } from '@layout/page-header/page-header';
import { RuleSection } from './rule-section/rule-section';
import {
  BOSS_CATEGORY_SHOWCASE,
  DIFFICULTY_SHOWCASE,
  MATCH_DAMAGE_SHOWCASE,
  MATCH_DECAY_SHOWCASE,
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
   * i18n service, read for the active language when formatting the squad multipliers.
   */
  private readonly translation = inject(Translation);

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
   * Daily diminishing-returns ladder shown in the damage panel, exposed to the template.
   */
  protected readonly matchDecayShowcase = MATCH_DECAY_SHOWCASE;

  /**
   * Squad bonus ladder, each tier rendered as the multiplier it applies to a challenge's damage.
   *
   * Formatted here rather than held formatted in the constants, so the decimal separator follows
   * the language the reader picked.
   */
  protected readonly teamBonusShowcase = computed(() => {
    const language = this.translation.language();

    return TEAM_BONUS_SHOWCASE.map((tier) => ({
      label: tier.label,
      multiplier: formatSquadMultiplier(tier.bonus, language) ?? '',
    }));
  });

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
