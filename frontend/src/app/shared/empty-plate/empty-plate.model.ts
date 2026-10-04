/**
 * Empty plate drawing: `radar` no campaign, `podium` nobody ranked, `draw` draw not run,
 * `matches` nobody played today.
 */
export type EmptyIllustration = 'radar' | 'podium' | 'draw' | 'matches';

/**
 * Readout dot: `live` running now, `todo` waits on someone, `info` plain fact.
 */
export type ReadoutTone = 'live' | 'todo' | 'info';

/**
 * Translated line of the plate's status strip.
 */
export interface EmptyReadout {
  /**
   * Dot tone.
   */
  readonly tone: ReadoutTone;

  /**
   * Translated label.
   */
  readonly label: string;

  /**
   * Value of the line.
   */
  readonly value: string;
}

/**
 * Translated empty plate content, built by the page that knows the situation.
 */
export interface EmptyPlate {
  /**
   * Drawing above the text.
   */
  readonly illustration: EmptyIllustration;

  /**
   * Caption over the title, optional.
   */
  readonly eyebrow?: string;

  /**
   * Plate title.
   */
  readonly title: string;

  /**
   * Explanation under the title.
   */
  readonly text: string;

  /**
   * Status lines under the text.
   */
  readonly readouts: readonly EmptyReadout[];
}
