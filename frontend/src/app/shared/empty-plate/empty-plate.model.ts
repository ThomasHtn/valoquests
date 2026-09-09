/**
 * Drawings an empty plate can carry, one per situation the public pages run into.
 *
 * - `radar` — a scan with nothing on it yet: the overview before a campaign exists.
 * - `road` — the ten planets in a line, dotted: the campaign page before a road is drawn.
 * - `podium` — three empty steps: a week nobody is ranked in.
 * - `draw` — a target and the five weekly slots: a week whose draw has not run.
 */
export type EmptyIllustration = 'radar' | 'road' | 'podium' | 'draw';

/**
 * How a readout's dot reads: `live` is running now, `todo` waits on someone, `info` is a plain fact.
 */
export type ReadoutTone = 'live' | 'todo' | 'info';

/**
 * One line of the plate's status strip: a label and its value, already translated.
 */
export interface EmptyReadout {
  /**
   * How the readout’s dot reads.
   */
  readonly tone: ReadoutTone;

  /**
   * Text shown to the reader, already translated.
   */
  readonly label: string;

  /**
   * Value of the line.
   */
  readonly value: string;
}

/**
 * Everything an empty plate shows, already translated.
 *
 * Built by the page, which knows the situation, and handed to `app-resource-state` (or rendered
 * straight through `app-empty-plate` where the empty case sits inside loaded content).
 */
export interface EmptyPlate {
  /**
   * Drawing shown above the text.
   */
  readonly illustration: EmptyIllustration;

  /**
   * Small caption over the title.
   */
  readonly eyebrow: string;

  /**
   * Title of the plate.
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
