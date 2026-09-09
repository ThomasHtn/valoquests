/**
 * The base at night, and the rocket being built in its middle.
 *
 * The state of the campaign, drawn: the city is the score, the rocket gains a stage per guardian
 * defeated, and what remains to be built stands there, dotted. Drawn imperatively into one
 * `<svg>` rather than templated: a few hundred nodes computed from two numbers are a drawing, not
 * a view, and a template of `@for` loops over generated geometry would say nothing a reader could
 * follow.
 */
/**
 * Sky kept above the drawing, in viewBox units.
 */
export const SCENE_HEADROOM = 40;
