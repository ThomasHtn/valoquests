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
import { RULE_ICONS, ICON_TONES, TOKEN } from './rule-text.constants';
import { RuleIcon, RuleRun } from './rule-text.model';

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
            const icon = name as RuleIcon;
            return { text: '', strong: false, icon, tone: ICON_TONES[icon] ?? 'text-brand-400' };
          }
        }
        if (part.startsWith('*') && part.endsWith('*') && part.length > 2) {
          return { text: part.slice(1, -1), strong: true, icon: null, tone: '' };
        }
        return { text: part, strong: false, icon: null, tone: '' };
      }),
  );
}
