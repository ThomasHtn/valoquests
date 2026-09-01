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
   * The two-character marker the section shows beside its title (`01`, `02`, … or `+`).
   */
  readonly index: string;

  /**
   * Key of the section's subtree under `rules.sections`, which resolves both its title and — through
   * `Translation.searchText` — every string the section renders.
   */
  readonly key: string;
}

/**
 * The rulebook's sections, in reading order.
 *
 * The page is 4 380 pixels tall and answered a question only to a reader willing to scroll it whole:
 * there was no table of contents, no anchor and no search, so a rule could be read but not looked
 * up. This list is what the contents rail draws, what the search filters, and what other screens
 * link into — one place, so a section cannot be renumbered on the page while a deep link keeps
 * pointing at the old one.
 *
 * `bonuses` sits between beats 02 and 03 and is marked `+` rather than numbered, on the page as
 * here: it is a multiplier on the two beats around it, not a beat of its own.
 */
export const RULE_ANCHORS: readonly RuleAnchor[] = [
  { id: 'challenges', index: '01', key: 'challenges' },
  { id: 'damage', index: '02', key: 'damage' },
  { id: 'bonuses', index: '+', key: 'bonuses' },
  { id: 'boss', index: '03', key: 'boss' },
  { id: 'calibration', index: '04', key: 'calibration' },
  { id: 'eligibility', index: '05', key: 'eligibility' },
  { id: 'ranking', index: '06', key: 'ranking' },
  { id: 'colony', index: '07', key: 'colony' },
  { id: 'night', index: '08', key: 'night' },
  { id: 'town', index: '09', key: 'town' },
];

/**
 * Fragments other screens link to, named rather than spelled out at each call site so a rename is
 * caught by the compiler instead of silently landing the reader at the top of the page.
 */
export const RULE_ANCHOR = {
  dailyYield: 'damage',
  colony: 'colony',
  ranking: 'ranking',
  town: 'town',
} as const;
