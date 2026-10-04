/**
 * Records in display order; also the translation key suffix.
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
   * Which record this is; picks the icon and the label.
   */
  readonly key: RecordKey;

  /**
   * Formatted record.
   */
  readonly value: string;

  /**
   * Translated tooltip saying where and when it was set.
   */
  readonly tooltip: string;

  /**
   * Rank badge replacing the icon, `null` for other records.
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
   * Translated tier name, the badge's alt text.
   */
  readonly label: string;
}
