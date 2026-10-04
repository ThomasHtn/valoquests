/**
 * Backend RFC 7807 problem response.
 */
export interface ApiProblem {
  /**
   * Human-readable explanation.
   */
  readonly detail?: string;

  /**
   * Validation messages, by field.
   */
  readonly errors?: Record<string, string>;
}
