/**
 * Translation keys of the data the campaign reset clears, listed for the operator before they
 * confirm.
 *
 * Spelled out rather than summarised as "everything": the reset keeps the roster and the
 * catalogues, and an operator who assumed otherwise would hesitate over an action that is in fact
 * safe for the players they set up.
 */
export const CLEARED_DATA_KEYS: readonly string[] = [
  'admin.maintenance.reset.cleared.matches',
  'admin.maintenance.reset.cleared.challenges',
  'admin.maintenance.reset.cleared.rankings',
  'admin.maintenance.reset.cleared.campaigns',
  'admin.maintenance.reset.cleared.synchronizations',
];

/**
 * Translation keys of what the campaign reset leaves untouched.
 */
export const KEPT_DATA_KEYS: readonly string[] = [
  'admin.maintenance.reset.kept.players',
  'admin.maintenance.reset.kept.challengeCatalogue',
  'admin.maintenance.reset.kept.guardianCatalogue',
];
