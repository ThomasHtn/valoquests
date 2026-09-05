/**
 * One entry of the rulebook's table of contents.
 */
export interface RuleAnchor {
  /**
   * Fragment identifying the section in a URL, and the `id` its heading carries.
   *
   * Written by hand rather than derived from the title: it goes into links other screens hold, so
   * it has to survive a rewording of the section it points at.
   */
  readonly id: string;

  /**
   * The two-digit marker the section shows beside its title.
   */
  readonly index: string;

  /**
   * Key of the section's subtree under `rules.sections`, which resolves both its title and, through
   * `Translation.searchText`, every string the section renders.
   */
  readonly key: string;
}

/**
 * The rulebook's sections, in reading order: the order of `docs/GAMEPLAY.md`, from the mission to
 * the table of constants.
 *
 * This list is what the contents rail draws, what the search filters, and what other screens link
 * into, so a section cannot be renumbered on the page while a deep link keeps pointing at the old
 * one.
 */
export const RULE_ANCHORS: readonly RuleAnchor[] = [
  { id: 'mission', index: '01', key: 'mission' },
  { id: 'resources', index: '02', key: 'resources' },
  { id: 'multipliers', index: '03', key: 'multipliers' },
  { id: 'day', index: '04', key: 'day' },
  { id: 'week', index: '05', key: 'week' },
  { id: 'campaign', index: '06', key: 'campaign' },
  { id: 'calibration', index: '07', key: 'calibration' },
  { id: 'challenges', index: '08', key: 'challenges' },
  { id: 'titles', index: '09', key: 'titles' },
  { id: 'rocket', index: '10', key: 'rocket' },
  { id: 'constants', index: '11', key: 'constants' },
];

/**
 * Fragments other screens link to, named rather than spelled out at each call site so a rename is
 * caught by the compiler instead of silently landing the reader at the top of the page.
 */
export const RULE_ANCHOR = {
  dailyYield: 'multipliers',
  week: 'week',
  challenges: 'challenges',
  calibration: 'calibration',
} as const;
