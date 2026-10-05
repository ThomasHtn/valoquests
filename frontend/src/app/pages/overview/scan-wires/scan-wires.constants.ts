import { Mark } from './scan-wires.model';

/**
 * Callouts to the guardian and stakes rows (stakes gone once the guardian is down). Points sit
 * inside the ringed globe (radius ~86); the upper one feeds the upper row so wires never cross.
 */
export const MARKS: readonly Mark[] = [
  // `--color-accent-red` and `--color-brand-500`: SVG attributes cannot read tokens.
  { vx: 232, vy: 128, card: 'guardian', tone: '#ff4655' },
  { vx: 215, vy: 150, card: 'stakes', tone: '#d9954a' },
];

/**
 * Dark stroke under each wire, `--color-night-sky`, so it reads over the planet's light face.
 */
export const WIRE_HALO = '#040a11';
