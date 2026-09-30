/**
 * Game modes that feed each stock the most, as the scoring splits a match's value.
 */
export const CARRY_MODES: readonly string[] = ['COMPETITIVE', 'PREMIER', 'UNRATED'];

/**
 * Game modes whose value goes mostly to food, the shelter stock.
 */
export const SHELTER_MODES: readonly string[] = [
  'DEATHMATCH',
  'SPIKE_RUSH',
  'TEAM_DEATHMATCH',
  'SKIRMISH',
  'SWIFTPLAY',
  'ESCALATION',
];

/**
 * Viewbox of the aboard dial's rocket, taller than wide; the dial keeps the same aspect ratio.
 */
export const HULL_VIEWBOX = '0 0 100 140';

/**
 * Outline of the aboard dial's rocket: ogive nose, straight hull, two fins and a flared skirt.
 */
export const HULL_PATH =
  'M50 2 C63 10 77 27 77 50 L77 98 L94 118 L94 137 L79 128 L82 138 L18 138 L21 128 L6 137' +
  ' L6 118 L23 98 L23 50 C23 27 37 10 50 2 Z';

/**
 * The same outline as a CSS mask, so the level rises inside the rocket and nowhere else.
 */
export const HULL_MASK = `url("data:image/svg+xml,${encodeURIComponent(
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${HULL_VIEWBOX}" preserveAspectRatio="none">` +
    `<path d="${HULL_PATH}"/></svg>`,
)}")`;
