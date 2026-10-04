import { AfterViewInit, Component, computed, DestroyRef, ElementRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  LucideBed,
  LucideCrown,
  LucideFlame,
  LucideHeartPulse,
  LucidePlay,
  LucideRefreshCw,
  LucideSkull,
  LucideTarget,
  LucideUsers,
  LucideWheat,
  LucideWrench,
  LucideZap,
} from '@lucide/angular';

import { resolveTitleVisual } from '@core/campaign/titles/campaign-title-visual.utils';
import { resolveDifficultyVisual } from '@core/challenges/visual/challenge-visual.utils';
import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { resolveLocale } from '@core/i18n/format/locale.utils';
import { formatNumber } from '@core/i18n/format/number-format.utils';
import { Translation } from '@core/i18n/translation';
import { RULE_ANCHOR } from '@core/rules/rule-anchor.constants';
import { PageHeader } from '@layout/page-header/page-header';
import { RuleSection } from './rule-section/rule-section';
import { RuleText } from './rule-text/rule-text';
import {
  CALIBRATION_FACT_KEYS,
  CAMPAIGN_WEEKS,
  CHALLENGE_WORTH,
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
  DIFFICULTY_BANDS,
  RULE_CONSTANTS,
  RULE_TITLES,
  WEEK_STEP_KEYS,
} from './rules.constants';
import { PAGE_LAYOUT_CLASS } from '@layout/page-layout.constants';

/**
 * Rules page: `docs/GAMEPLAY.md` in eight sections, with the document's own figures.
 */
@Component({
  selector: 'app-rules',
  imports: [
    TranslatePipe,
    RouterLink,
    LucideBed,
    LucideCrown,
    LucideFlame,
    LucideHeartPulse,
    LucidePlay,
    LucideRefreshCw,
    LucideSkull,
    LucideTarget,
    LucideUsers,
    LucideWheat,
    LucideWrench,
    LucideZap,
    PageHeader,
    RuleSection,
    RuleText,
  ],
  templateUrl: './rules.html',
  styleUrl: './rules.scss',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class Rules implements AfterViewInit {
  /**
   * Translation service, whose language picks the number notation.
   */
  private readonly translation = inject(Translation);

  /**
   * Page element, searched for the section a fragment names.
   */
  private readonly host = inject(ElementRef<HTMLElement>);

  /**
   * Active route, whose fragment names the section to scroll to.
   */
  private readonly route = inject(ActivatedRoute);

  /**
   * Destroy hook, to stop following the fragment.
   */
  private readonly destroyRef = inject(DestroyRef);

  /**
   * Section ids, shared with the links other pages point at.
   */
  protected readonly anchor = RULE_ANCHOR;

  /**
   * Reference the worked examples use.
   */
  protected readonly exampleReference = EXAMPLE_REFERENCE;

  /**
   * Active operators of the squad the campaign example is sized for.
   */
  protected readonly exampleOperators = EXAMPLE_OPERATORS;

  /**
   * Game modes grouped by the share of a match going to food.
   */
  protected readonly modeGroups = MODE_GROUPS;

  /**
   * Daily diminishing returns, by rank of the match in the day.
   */
  protected readonly decayLadder = DECAY_LADDER;

  /**
   * Bonus per day played in the week.
   */
  protected readonly streakLadder = STREAK_LADDER;

  /**
   * What a day of the week does, in order.
   */
  protected readonly weekStepKeys = WEEK_STEP_KEYS;

  /**
   * Limits of Sunday's extraction, then what they add up to.
   */
  protected readonly sundayTermKeys = SUNDAY_TERM_KEYS;

  /**
   * Worked example of one Sunday settlement, line by line.
   */
  protected readonly sundayExample = SUNDAY_EXAMPLE;

  /**
   * Share of the base lost per breakthrough level when the guardian stands.
   */
  protected readonly guardianLossLadder = GUARDIAN_LOSS_LADDER;

  /**
   * The campaign's life, in order.
   */
  protected readonly lifecycleKeys = LIFECYCLE_KEYS;

  /**
   * The two difficulties and their references.
   */
  protected readonly difficultyBands = DIFFICULTY_BANDS;

  /**
   * How the difficulty is decided, in the document's order.
   */
  protected readonly calibrationFactKeys = CALIBRATION_FACT_KEYS;

  /**
   * Closing sheet of the constants a player can picture.
   */
  protected readonly constants = RULE_CONSTANTS;

  /**
   * The champion and the four weekly titles, with the ranking's icons and colours.
   */
  protected readonly titles = RULE_TITLES.map((key) => ({ key, ...resolveTitleVisual(key) }));

  /**
   * What each challenge is worth, with the tier's own colour and numeral.
   */
  protected readonly challengeWorth = CHALLENGE_WORTH.map((worth) => ({
    ...worth,
    visual: resolveDifficultyVisual(worth.difficulty),
  }));

  /**
   * The ten weeks sized for the example squad, hit points and wounded rounded.
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

  /**
   * Locale of the active language, for the number formats.
   */
  private readonly locale = computed(() => resolveLocale(this.translation.language()));

  /**
   * Guardian factor in the reader's notation, quoted so the text cannot drift from the table.
   */
  protected readonly guardianFactor = computed(() =>
    formatNumber(GUARDIAN_FACTOR, this.locale(), { minimumFractionDigits: 2 }),
  );

  /**
   * Groups an amount in the reader's notation.
   */
  protected amount(value: number): string {
    return formatDamage(value, this.translation.language());
  }

  /**
   * Formats a 0 to 100 percentage, decimals kept (`0.004 %`), in the reader's notation.
   */
  protected percent(value: number): string {
    return formatNumber(value / 100, this.locale(), {
      style: 'percent',
      maximumFractionDigits: 3,
    });
  }

  /**
   * Formats a challenge's weight, `× 1.7`.
   */
  protected times(value: number): string {
    const formatted = formatNumber(value, this.locale(), {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    });
    return `× ${formatted}`;
  }

  /**
   * Scrolls to the fragment by hand: `anchorScrolling` scrolls the document, not `page-body`.
   */
  public ngAfterViewInit(): void {
    this.route.fragment.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((fragment) => {
      if (fragment !== null) {
        this.scrollTo(fragment);
      }
    });
  }

  /**
   * Smoothly scrolls the section with id `anchor` into view.
   */
  private scrollTo(anchor: string): void {
    const target = this.host.nativeElement.querySelector(`#${CSS.escape(anchor)}`);
    target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}
