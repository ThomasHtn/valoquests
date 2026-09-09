import { Mark } from './scan-wires.model';

/**
 * A single callout: the wounded on the ground. The other readings point at nothing on the planet,
 * so they stay unwired.
 */
export const MARKS: readonly Mark[] = [{ vx: 215, vy: 150, card: 'ground', tone: '#d9954a' }];
