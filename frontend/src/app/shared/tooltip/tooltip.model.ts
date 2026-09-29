/**
 * Side of the host the tooltip is rendered on.
 */
export type TooltipPosition = 'above' | 'below' | 'left' | 'right';

/**
 * One SVG element of an icon drawn by hand inside a tooltip.
 */
export interface TooltipIconNode {
  /**
   * SVG tag name.
   */
  readonly tag: 'path' | 'circle';

  /**
   * Geometry attributes of the element.
   */
  readonly attributes: Readonly<Record<string, string>>;
}

/**
 * What opens the tooltip: resting on or focusing the host, or mouse hover plus tap for an info button.
 */
export type TooltipTrigger = 'hover' | 'click';
