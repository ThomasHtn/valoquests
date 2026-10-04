/**
 * Side of the host the tooltip sits on.
 */
export type TooltipPosition = 'above' | 'below' | 'left' | 'right';

/**
 * One SVG element of a hand-built tooltip icon.
 */
export interface TooltipIconNode {
  /**
   * SVG tag name.
   */
  readonly tag: 'path' | 'circle';

  /**
   * Geometry attributes.
   */
  readonly attributes: Readonly<Record<string, string>>;
}

/**
 * `hover` for hover and focus, `click` for an info button (mouse hover plus tap).
 */
export type TooltipTrigger = 'hover' | 'click';
