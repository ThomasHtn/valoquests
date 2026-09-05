import { AfterViewInit, Component, computed, DestroyRef, ElementRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  LucideFlame,
  LucideHeartPulse,
  LucidePlay,
  LucideSkull,
  LucideTarget,
  LucideWheat,
  LucideWrench,
  LucideZap,
} from '@lucide/angular';

import { WEEKLY_TITLES } from '@core/campaign/campaign.model';
import { resolveTitleVisual } from '@core/campaign/campaign-visual.utils';
import { resolveDifficultyVisual } from '@core/challenges/challenge-visual.utils';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { RULE_ANCHOR } from '@core/rules/rule-anchor.constants';
import { PageHeader } from '@layout/page-header/page-header';
import { RuleSection } from './rule-section/rule-section';
import { RuleText } from './rule-text/rule-text';
import {
  CALIBRATION_FACT_KEYS,
  CAMPAIGN_WEEKS,
  CHALLENGE_WORTH,
  CONSTANT_KEYS,
  DECAY_LADDER,
  EXAMPLE_OPERATORS,
  EXAMPLE_REFERENCE,
  GROUP_FACTOR,
  GUARDIAN_FACTOR,
  GUARDIAN_LOSS_LADDER,
  LIFECYCLE_KEYS,
  MODE_GROUPS,
  PROGRESSION_PER_WEEK,
  STREAK_LADDER,
  SUNDAY_EXAMPLE,
  SUNDAY_TERM_KEYS,
  TIER_BANDS,
  WEEK_STEP_KEYS,
} from './rules.constants';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * Rules page.
 *
 * `docs/GAMEPLAY.md` in eight numbered sections, each stating its rule in one sentence and
 * pairing a short description with the figures behind it, for anyone landing on the tracker
 * without prior context. The figures are the document's, not a campaign's: the page explains the
 * game as written, and its worked examples are sized on the squad the document itself uses.
 */
@Component({
  selector: 'app-rules',
  imports: [
    TranslatePipe,
    RouterLink,
    LucideFlame,
    LucideHeartPulse,
    LucidePlay,
    LucideSkull,
    LucideTarget,
    LucideWheat,
    LucideWrench,
    LucideZap,
    PageHeader,
    RuleSection,
    RuleText,
  ],
  templateUrl: './rules.html',
  styleUrl: './rules.css',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Rules implements AfterViewInit {
  private readonly translation = inject(Translation);

  private readonly host = inject(ElementRef<HTMLElement>);

  private readonly route = inject(ActivatedRoute);

  private readonly destroyRef = inject(DestroyRef);

  protected readonly anchor = RULE_ANCHOR;

  protected readonly exampleReference = EXAMPLE_REFERENCE;

  protected readonly exampleOperators = EXAMPLE_OPERATORS;

  protected readonly modeGroups = MODE_GROUPS;

  protected readonly decayLadder = DECAY_LADDER;

  protected readonly streakLadder = STREAK_LADDER;

  protected readonly weekStepKeys = WEEK_STEP_KEYS;

  protected readonly sundayTermKeys = SUNDAY_TERM_KEYS;

  protected readonly sundayExample = SUNDAY_EXAMPLE;

  protected readonly guardianLossLadder = GUARDIAN_LOSS_LADDER;

  protected readonly lifecycleKeys = LIFECYCLE_KEYS;

  protected readonly tierBands = TIER_BANDS;

  protected readonly calibrationFactKeys = CALIBRATION_FACT_KEYS;

  protected readonly constantKeys = CONSTANT_KEYS;

  /**
   * The four titles with their icon and colour, the ranking's own.
   */
  protected readonly titles = WEEKLY_TITLES.map((key) => ({ key, ...resolveTitleVisual(key) }));

  /**
   * What each challenge is worth, with the tier's own colour and numeral.
   */
  protected readonly challengeWorth = CHALLENGE_WORTH.map((worth) => ({
    ...worth,
    visual: resolveDifficultyVisual(worth.difficulty),
  }));

  /**
   * The ten weeks sized for the example squad: the document's two weights turned into the hit
   * points and the wounded a reader can picture, rounded to the hundred and to the ten.
   */
  protected readonly campaignWeeks = CAMPAIGN_WEEKS.map((week, index) => {
    const weekly = EXAMPLE_REFERENCE * EXAMPLE_OPERATORS;
    const progression = 1 + PROGRESSION_PER_WEEK * index;
    return {
      ...week,
      number: index + 1,
      hitPoints: Math.round((weekly * GUARDIAN_FACTOR * week.guardian) / 100) * 100,
      wounded: Math.round((weekly * GROUP_FACTOR * week.group * progression) / 10) * 10,
    };
  });

  /**
   * The colour a guardian's category is drawn in, the campaign page's own.
   */
  protected readonly categoryClass: Readonly<Record<string, string>> = {
    MINOR: 'text-accent-green',
    STANDARD: 'text-brand-400',
    ELITE: 'text-boss-hp-edge',
  };

  private readonly locale = computed(() =>
    this.translation.language() === 'fr' ? 'fr-FR' : 'en-GB',
  );

  /**
   * Groups an amount in the reader's own notation, as every other screen quoting the rulebook.
   *
   * @param value - The raw amount.
   * @returns The grouped amount.
   */
  protected amount(value: number): string {
    return formatDamage(value, this.translation.language());
  }

  /**
   * Formats a percentage that may carry decimals, `0,004 %` as well as `35 %`.
   *
   * @param value - The percentage.
   * @returns The formatted percentage, sign included.
   */
  protected percent(value: number): string {
    const digits = new Intl.NumberFormat(this.locale(), { maximumFractionDigits: 3 }).format(value);
    return `${digits} %`;
  }

  /**
   * Formats a challenge's weight, `× 1,7`.
   *
   * @param value - The multiplier.
   * @returns The formatted multiplier.
   */
  protected times(value: number): string {
    const formatted = new Intl.NumberFormat(this.locale(), {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(value);
    return `× ${formatted}`;
  }

  /**
   * Honours the fragment the reader arrived on.
   *
   * Scrolled by hand rather than through the router's `anchorScrolling`, which scrolls the
   * document: this application's document never scrolls, every page's own `page-body` does.
   */
  public ngAfterViewInit(): void {
    this.route.fragment.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((fragment) => {
      if (fragment !== null) {
        this.scrollTo(fragment);
      }
    });
  }

  private scrollTo(anchor: string): void {
    const target = this.host.nativeElement.querySelector(`#${CSS.escape(anchor)}`);
    target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}
