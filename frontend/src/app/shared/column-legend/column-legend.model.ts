/**
 * One column a legend explains.
 */
export interface ColumnLegendEntry {
  /**
   * Already-translated column name, as its caption reads.
   */
  readonly label: string;

  /**
   * Already-translated explanation of what the column counts.
   */
  readonly help: string;
}
