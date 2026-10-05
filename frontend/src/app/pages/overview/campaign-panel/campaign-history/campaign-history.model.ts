import { Concept } from '@core/concepts/concept.model';

/**
 * A table column headed by a concept icon.
 */
export interface HistoryColumn {
  /**
   * Concept whose icon and colour head the column.
   */
  readonly concept: Extract<Concept, 'base' | 'guardian' | 'wounded'>;

  /**
   * Translation key of the column name.
   */
  readonly labelKey: string;
}
