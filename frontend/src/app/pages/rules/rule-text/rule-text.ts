import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import {
  LucideCalendar,
  LucideFlame,
  LucideHeartPulse,
  LucideRocket,
  LucideSkull,
  LucideSwords,
  LucideTarget,
  LucideUsers,
  LucideWheat,
  LucideWrench,
  LucideZap,
} from '@lucide/angular';

/**
 * Icons a rule may name inline, by the token written in the dictionary (`{food}`, `{guardian}`).
 *
 * The same vocabulary as the rest of the gameplay pages: a wheat ear is food everywhere, a skull is
 * the guardian everywhere. A token outside this list is rendered as its own text, so a typo in a
 * dictionary shows up on screen rather than vanishing.
 */
const RULE_ICONS = [
  'food',
  'components',
  'damage',
  'guardian',
  'wounded',
  'base',
  'challenge',
  'streak',
  'points',
  'rocket',
  'day',
] as const;

type RuleIcon = (typeof RULE_ICONS)[number];

/**
 * One run of a rule's text: plain words, a word set in relief, or an icon standing for a word.
 */
interface RuleRun {
  readonly text: string;
  readonly strong: boolean;
  readonly icon: RuleIcon | null;
}

const TOKEN = /(\{[a-z]+\}|\*[^*]+\*)/;

/**
 * A sentence of the rulebook with its icons and emphasis in place.
 *
 * The dictionary writes `{food}` where the wheat ear goes and `*so*` around the words to set in
 * relief; the component turns that into text runs and inline icons. Parsed into runs rather than
 * bound as HTML, so nothing from the dictionary ever reaches `innerHTML`.
 */
@Component({
  selector: 'app-rule-text',
  imports: [
    LucideCalendar,
    LucideFlame,
    LucideHeartPulse,
    LucideRocket,
    LucideSkull,
    LucideSwords,
    LucideTarget,
    LucideUsers,
    LucideWheat,
    LucideWrench,
    LucideZap,
  ],
  templateUrl: './rule-text.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RuleText {
  /**
   * Already-translated sentence, with `{icon}` and `*emphasis*` tokens.
   */
  public readonly text = input.required<string>();

  protected readonly runs = computed<readonly RuleRun[]>(() =>
    this.text()
      .split(TOKEN)
      .filter((part) => part.length > 0)
      .map((part) => {
        if (part.startsWith('{') && part.endsWith('}')) {
          const name = part.slice(1, -1);
          if ((RULE_ICONS as readonly string[]).includes(name)) {
            return { text: '', strong: false, icon: name as RuleIcon };
          }
        }
        if (part.startsWith('*') && part.endsWith('*') && part.length > 2) {
          return { text: part.slice(1, -1), strong: true, icon: null };
        }
        return { text: part, strong: false, icon: null };
      }),
  );
}
