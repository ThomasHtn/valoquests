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
import { PageHeader } from '@layout/page-header/page-header';
import { RuleContents } from './rule-contents/rule-contents';
import { RuleSection } from './rule-section/rule-section';
import { RuleText } from './rule-text/rule-text';
import {
  CAMPAIGN_WEEKS,
  CHALLENGE_WORTH,
  CONSTANT_KEYS,
  DAY_STEP_KEYS,
  DECAY_LADDER,
  EXAMPLE_REFERENCE,
  FAMINE_LADDER,
  GUARDIAN_LOSS_LADDER,
  LOOP_KEYS,
  MATCH_DAMAGE,
  REFERENCE_FLOOR,
  RESOURCE_EXAMPLES,
  STREAK_LADDER,
  STREAK_RULE_KEYS,
  TIER_BANDS,
  UPKEEP_LADDER,
} from './rules.constants';
import { PAGE_LAYOUT_CLASS } from '../page-layout.constants';

/**
 * Rules page.
 *
 * `docs/GAMEPLAY.md`, read top to bottom: one section per chapter, each pairing a short narrative
 * with the numbers behind it, for anyone landing on the tracker without prior context. The figures
 * are the document's, not a campaign's: the page explains the game as written.
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
    RuleContents,
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

  /**
   * Fragment of the section on screen, marked in the contents rail.
   */
  protected readonly activeAnchor = signal<string | null>(null);

  protected readonly exampleReference = EXAMPLE_REFERENCE;

  protected readonly referenceFloor = REFERENCE_FLOOR;

  protected readonly loopKeys = LOOP_KEYS;

  protected readonly matchDamage = MATCH_DAMAGE;

  protected readonly resourceExamples = RESOURCE_EXAMPLES;

  protected readonly decayLadder = DECAY_LADDER;

  protected readonly streakLadder = STREAK_LADDER;

  protected readonly streakRuleKeys = STREAK_RULE_KEYS;

  protected readonly dayStepKeys = DAY_STEP_KEYS;

  protected readonly upkeepLadder = UPKEEP_LADDER;

  protected readonly famineLadder = FAMINE_LADDER;

  protected readonly guardianLossLadder = GUARDIAN_LOSS_LADDER;

  protected readonly campaignWeeks = CAMPAIGN_WEEKS;

  protected readonly tierBands = TIER_BANDS;

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
   * Formats a weight, `× 1,30`.
   *
   * @param value - The multiplier.
   * @returns The formatted multiplier.
   */
  protected times(value: number): string {
    const digits = new Intl.NumberFormat(this.locale(), {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
    return `× ${digits}`;
  }

  /**
   * Starts following the reader's scroll, and honours the fragment they arrived on.
   *
   * Scrolled by hand rather than through the router's `anchorScrolling`, which scrolls the
   * document: this application's document never scrolls, every page's own `page-body` does.
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
   * Follows which section is on screen, so the contents rail can mark it. Rooted on `page-body`,
   * the scroller; the bottom margin pulls the observation line up to the top third of the column.
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
