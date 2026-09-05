/**
 * Fragments of the rules page other screens link to, named rather than spelled out at each call
 * site so a rename is caught by the compiler instead of silently landing the reader at the top of
 * the page.
 *
 * Each value is the `id` a numbered section of `pages/rules` carries. Written by hand rather than
 * derived from the section's title: it goes into links other screens hold, so it has to survive a
 * rewording of the section it points at.
 */
export const RULE_ANCHOR = {
  match: 'match',
  dailyYield: 'multipliers',
  week: 'week',
  sunday: 'sunday',
  campaign: 'campaign',
  calibration: 'calibration',
  challenges: 'challenges',
  constants: 'constants',
} as const;
