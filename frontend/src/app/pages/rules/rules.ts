import {
  AfterViewInit,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucidePlay } from '@lucide/angular';

import { BossCategory } from '@core/boss/boss.model';
import { BOSS_CATEGORY_LADDER, resolveBossCategoryColorClass } from '@core/boss/boss-visual.utils';
import { formatDamage, formatSquadMultiplier } from '@core/challenges/challenge-format.utils';
import { formatGauge } from '@core/colony/colony-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { ChampionBadge } from '@shared/champion-badge/champion-badge';
import { PageHeader } from '@layout/page-header/page-header';
import { RuleContents } from './rule-contents/rule-contents';
import { RuleSection } from './rule-section/rule-section';
import {
  BOSS_CATEGORY_SHOWCASE,
  BOSS_CATEGORY_WEIGHT_SHARE,
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
  imports: [
    TranslatePipe,
    RouterLink,
    ChampionBadge,
    LucidePlay,
    PageHeader,
    RuleContents,
    RuleSection,
  ],
  templateUrl: './rules.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Rules implements AfterViewInit {
  /**
   * i18n service, read for the active language when formatting the squad multipliers.
   */
  private readonly translation = inject(Translation);

  /**
   * The reader's own view of the page, used to reach the scroll container the sections live in.
   */
  private readonly host = inject(ElementRef<HTMLElement>);

  /**
   * Route, read once for the fragment a deep link from another screen arrives with.
   */
  private readonly route = inject(ActivatedRoute);

  /**
   * Teardown hook for the scroll observer.
   */
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Fragment of the section on screen, marked in the contents rail.
   */
  protected readonly activeAnchor = signal<string | null>(null);

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
  protected readonly bossLadderShowcase = BOSS_CATEGORY_LADDER;

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

  /**
   * Height a week's bar is drawn at in the ladder, as a share of the heaviest class.
   *
   * @param category - The week's weight class.
   * @returns The bar's height, in percent of its track.
   */
  protected bossWeightShare(category: BossCategory): number {
    return BOSS_CATEGORY_WEIGHT_SHARE[category];
  }

  /**
   * Groups one of the barème's amounts, in the reader's own notation.
   *
   * Same reason as {@link colonyTierShowcase}: bound raw, the ladder printed `1400` here while the
   * quest board printed `1 400` for that exact challenge, and the two screens stopped looking like
   * they were quoting one rulebook.
   *
   * @param value - The raw amount from the showcase constants.
   * @returns The grouped amount.
   */
  protected amount(value: number): string {
    return formatDamage(value, this.translation.language());
  }

  /**
   * Starts following the reader's scroll, and honours the fragment they arrived on.
   *
   * The fragment is watched rather than read once: the sidebar's submenu links here while the page
   * is already open, and a same-page fragment change re-runs no lifecycle hook — read from the
   * snapshot, the second jump of a session would do nothing.
   *
   * Scrolled by hand rather than through the router's `anchorScrolling`, which scrolls the
   * *document*: this application's document never scrolls, every page's own `page-body` does, so
   * the router would find the element and then scroll a window pinned at zero.
   */
  public ngAfterViewInit(): void {
    this.observeSections();

    this.route.fragment.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((fragment) => {
      if (fragment !== null) {
        this.scrollTo(fragment);
      }
    });
  }

  /**
   * Scrolls a section into view and marks it current.
   *
   * @param anchor - Fragment of the section to reveal.
   */
  protected scrollTo(anchor: string): void {
    const target = this.host.nativeElement.querySelector(`#${CSS.escape(anchor)}`);
    if (target === null) {
      return;
    }

    target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    this.activeAnchor.set(anchor);
  }

  /**
   * Follows which section is on screen, so the contents rail can mark it.
   *
   * Rooted on `page-body` rather than on the viewport, for the same reason the jump above is manual:
   * that element is the scroller. The bottom margin pulls the observation line up to the top third
   * of the column, so a section counts as "the one being read" once its heading reaches there rather
   * than when its last pixel leaves — which is what a reader means by it.
   */
  private observeSections(): void {
    const root = this.host.nativeElement.querySelector('.page-body');
    const sections = [...this.host.nativeElement.querySelectorAll('section[id]')];
    if (root === null || sections.length === 0) {
      return;
    }

    const observer = new IntersectionObserver(
      (entries) => {
        const onScreen = entries
          .filter((entry) => entry.isIntersecting)
          .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);

        if (onScreen.length > 0) {
          this.activeAnchor.set(onScreen[0].target.id);
        }
      },
      { root, rootMargin: '0px 0px -66% 0px', threshold: 0 },
    );

    sections.forEach((section) => observer.observe(section));
    this.destroyRef.onDestroy(() => observer.disconnect());
  }
}
