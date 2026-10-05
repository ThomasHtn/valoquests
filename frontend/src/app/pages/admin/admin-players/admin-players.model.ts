/**
 * Link closing a getting-started step.
 */
export interface GuideStepLink {
  /**
   * Router path of the page the step sends to.
   */
  readonly route: string;

  /**
   * Translation key of the link text.
   */
  readonly labelKey: string;
}

/**
 * One numbered getting-started step.
 */
export interface GuideStep {
  /**
   * Translation key of the instruction.
   */
  readonly textKey: string;

  /**
   * Link to the page that carries the step out, `null` when it needs none.
   */
  readonly link: GuideStepLink | null;
}
