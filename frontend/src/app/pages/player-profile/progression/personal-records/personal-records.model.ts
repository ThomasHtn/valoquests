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

  /**
   * Rank badge shown in place of the record's icon, or `null` for records without one.
   */
  readonly rankIcon: RecordRankIcon | null;
}

/**
 * Rank badge of the peak tier record.
 */
export interface RecordRankIcon {
  /**
   * Path of the tier's SVG badge.
   */
  readonly src: string | null;

  /**
   * Translated tier name, used as the badge's alt text.
   */
  readonly label: string;
}
