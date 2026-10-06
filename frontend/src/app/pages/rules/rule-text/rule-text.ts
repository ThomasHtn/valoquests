import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { LucideDynamicIcon } from '@lucide/angular';

import { CONCEPT_ICONS } from '@core/concepts/concept.constants';

import { RuleRun } from './rule-text.model';
import { parseRuleText } from './rule-text.utils';

/**
 * Rulebook sentence with `{icon}` and `*relief*` tokens, parsed so nothing reaches `innerHTML`.
 */
@Component({
  selector: 'app-rule-text',
  imports: [LucideDynamicIcon],
  templateUrl: './rule-text.html',
  styleUrl: './rule-text.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RuleText {
  /**
   * The one icon of each concept; a rule token names a concept.
   */
  protected readonly concepts = CONCEPT_ICONS;

  /**
   * Already-translated sentence, with `{icon}` and `*relief*` tokens.
   */
  public readonly text = input.required<string>();

  /**
   * Sentence split into plain, emphasized and icon runs for the template.
   */
  protected readonly runs = computed<readonly RuleRun[]>(() => parseRuleText(this.text()));
}
