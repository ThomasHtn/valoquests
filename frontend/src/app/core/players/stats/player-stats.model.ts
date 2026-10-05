/**
 * Colour of a judged statistic, shared by its figure and its bar.
 */
export interface StatVisual {
  /**
   * CSS colour, bound as `--tone` and read by the consumer's stylesheet.
   */
  readonly tone: string;
}
