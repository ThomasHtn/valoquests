/**
 * Game modes whose value goes mostly to components, the carry stock.
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
 * Viewbox of the aboard dial's rocket; the dial keeps its aspect ratio.
 */
export const HULL_VIEWBOX = '0 0 100 140';

/**
 * Outline of the aboard dial's rocket.
 */
export const HULL_PATH =
  'M50 2 C63 10 77 27 77 50 L77 98 L94 118 L94 137 L79 128 L82 138 L18 138 L21 128 L6 137' +
  ' L6 118 L23 98 L23 50 C23 27 37 10 50 2 Z';

/**
 * Same outline as a CSS mask, so the level rises only inside the rocket.
 */
export const HULL_MASK = `url("data:image/svg+xml,${encodeURIComponent(
  `<svg xmlns="http://www.w3.org/2000/svg" viewBox="${HULL_VIEWBOX}" preserveAspectRatio="none">` +
    `<path d="${HULL_PATH}"/></svg>`,
)}")`;

/**
 * Aboard figure font size by digit count, so it stays inside the 54-unit hull.
 */
export const HULL_FIGURE_SIZES: readonly { readonly maxDigits: number; readonly size: string }[] = [
  { maxDigits: 2, size: '2rem' },
  { maxDigits: 3, size: '1.875rem' },
  { maxDigits: 4, size: '1.25rem' },
  { maxDigits: 5, size: '1.0625rem' },
];

/**
 * Font size of the aboard figure beyond {@link HULL_FIGURE_SIZES}.
 */
export const HULL_FIGURE_MIN_SIZE = '0.875rem';
