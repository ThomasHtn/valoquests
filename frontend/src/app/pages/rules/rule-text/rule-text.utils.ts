import { RULE_ICON_MODIFIERS, RULE_ICONS, RULE_TOKEN_PATTERN } from './rule-text.constants';
import { RuleIcon, RuleRun } from './rule-text.model';

/**
 * Splits a translated rule into plain, emphasized and icon runs.
 */
export function parseRuleText(text: string): RuleRun[] {
  return text
    .split(RULE_TOKEN_PATTERN)
    .filter((part) => part.length > 0)
    .map(toRun);
}

/**
 * Reads one split part; an unknown `{token}` stays plain text so the typo shows.
 */
function toRun(part: string): RuleRun {
  const icon = part.startsWith('{') && part.endsWith('}') ? part.slice(1, -1) : null;
  if (icon !== null && isRuleIcon(icon)) {
    return { text: '', strong: false, icon, modifier: RULE_ICON_MODIFIERS[icon] ?? '' };
  }
  if (part.startsWith('*') && part.endsWith('*') && part.length > 2) {
    return { text: part.slice(1, -1), strong: true, icon: null, modifier: '' };
  }
  return { text: part, strong: false, icon: null, modifier: '' };
}

/**
 * Whether `name` is one of the inline icon tokens.
 */
function isRuleIcon(name: string): name is RuleIcon {
  return (RULE_ICONS as readonly string[]).includes(name);
}
