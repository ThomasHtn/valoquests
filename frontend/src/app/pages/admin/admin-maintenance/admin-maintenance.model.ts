/**
 * Fate of a group of records on a campaign reset, which picks its colours.
 */
export type ResetDataTone = 'cleared' | 'kept';

/**
 * One column of the reset panel: what the reset clears, or what it keeps.
 */
export interface ResetDataGroup {
  /**
   * Whether the reset clears or keeps these records.
   */
  readonly tone: ResetDataTone;

  /**
   * Translation key of the column caption.
   */
  readonly labelKey: string;

  /**
   * Translation keys of the records, one list item each.
   */
  readonly itemKeys: readonly string[];
}
