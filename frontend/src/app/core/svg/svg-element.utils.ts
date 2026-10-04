import { SVG_NS } from './svg-element.constants';
import { SvgAttrs } from './svg-element.model';

/**
 * Creates an SVG element with its attributes.
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
