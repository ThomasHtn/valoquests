import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucidePlay } from '@lucide/angular';

import { resolveBossCategoryColorClass } from '@core/boss/boss-visual.utils';
import { formatSquadMultiplier } from '@core/challenges/challenge-format.utils';
import { formatGauge } from '@core/colony/colony-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { PageHeader } from '@layout/page-header/page-header';
import { RuleSection } from './rule-section/rule-section';
import {
  BOSS_CATEGORY_SHOWCASE,
  BOSS_LADDER_SHOWCASE,
  BOSS_MATERIALS_SHOWCASE,
  COLONY_FACTS,
  COLONY_MATERIALS_SHOWCASE,
  COLONY_MORALE_SHOWCASE,
  COLONY_TIER_SHOWCASE,
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
 * Static walkthrough of the whole game, read top to bottom: one section per beat, each pairing a
 * short narrative with the numbers behind it, for anyone landing on the tracker without prior
 * context.
 *
 * Two halves. The first six beats are the weekly ranking: what a match is worth, what a challenge is
 * worth, and how the week is scored. The last three are the colony, which reads all of that a second
 * time and turns it into a town — the same damage, the same challenges and the same fights, priced
 * in food and efficiency instead of in points.
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
   * The weight class each week of a run fights, shown under the boss panel.
   */
  protected readonly bossLadderShowcase = BOSS_LADDER_SHOWCASE;

  /**
   * Materials a defeated boss pays per player, shown in the colony panel.
   */
  protected readonly bossMaterialsShowcase = BOSS_MATERIALS_SHOWCASE;

  /**
   * Facts closing the ranking panel, exposed to the template.
   */
  protected readonly rankingFacts = RANKING_FACTS;

  /**
   * The four numbers the colony's chain is made of, exposed to the template.
   */
  protected readonly colonyFacts = COLONY_FACTS;

  /**
   * Materials a completed challenge is worth, shown in the colony panel.
   */
  protected readonly colonyMaterialsShowcase = COLONY_MATERIALS_SHOWCASE;

  /**
   * What a Monday's fight moves the morale by, shown in the colony panel.
   */
  protected readonly colonyMoraleShowcase = COLONY_MORALE_SHOWCASE;

  /**
   * The town's first named tiers, shown in the colony panel.
   *
   * The efficiency each opens at is formatted here rather than in the template: the steps fall on
   * quarters, so a raw binding printed `8.75` under a French dictionary.
   */
  protected readonly colonyTierShowcase = computed(() =>
    COLONY_TIER_SHOWCASE.map((tier) => ({
      name: tier.name,
      threshold: formatGauge(tier.threshold, this.translation.language()),
    })),
  );

  /**
   * Resolves a boss category's text color utility, exposed to the template.
   */
  protected readonly bossCategoryColorClass = resolveBossCategoryColorClass;
}
