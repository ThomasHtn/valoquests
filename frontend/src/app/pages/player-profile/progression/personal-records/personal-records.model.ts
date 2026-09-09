/**
 * Every record the section can show, in display order. Doubles as the translation key suffix.
 */
export type RecordKey =
  | 'mostKills'
  | 'bestAcs'
  | 'mostDamage'
  | 'bestKda'
  | 'bestHeadshotPercentage'
  | 'longestWinStreak'
  | 'longestActiveDayStreak'
  | 'mvps'
  | 'peakTier';

/**
 * One record, ready to render.
 */
export interface RecordTile {
  /**
   * Which record this is; picks both the icon and the label.
   */
  readonly key: RecordKey;

  /**
   * The record itself, already formatted.
   */
  readonly value: string;

  /**
   * Already-translated explanation shown on the label, carrying where and when it was set.
   */
  readonly tooltip: string;
}
