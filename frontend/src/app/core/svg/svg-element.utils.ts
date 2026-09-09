/**
 * SVG DOM helpers shared by every drawing built by script (rocket, planets, skies, callout wires).
 *
 * Kept free of Angular so a drawing module stays a plain function of its inputs.
 */

/**
 * XML namespace every SVG node must be created under; `document.createElement` would yield an
 * inert HTML element instead.
 */
export const SVG_NS = 'http://www.w3.org/2000/svg';

/**
 * Attribute bag accepted by {@link svgElement}; numbers are stringified on the way in.
 */
export type SvgAttrs = Readonly<Record<string, string | number>>;

/**
 * Creates one SVG element with its attributes set.
 */
export function svgElement<K extends keyof SVGElementTagNameMap>(
  name: K,
  attrs: SvgAttrs = {},
): SVGElementTagNameMap[K] {
  const node = document.createElementNS(SVG_NS, name);
  for (const [key, value] of Object.entries(attrs)) {
    node.setAttribute(key, String(value));
  }
  return node;
}
