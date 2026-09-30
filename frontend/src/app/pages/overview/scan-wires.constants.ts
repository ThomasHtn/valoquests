import { Mark } from './scan-wires.model';

/**
 * Two callouts, in the order the week is played: the planet the guardian holds to the fight, the
 * wounded on the ground to what Sunday can still bring home. The second has nothing to land on once
 * the guardian is down, and is then left out.
 *
 * Both marks sit on the globe, inside even the ringed weeks' smaller one (radius ~86 in the
 * drawing), the guardian's one up and to the right of the wounded one. Both wires run a flat stub
 * then a 45° diagonal, so with the upper row fed from the upper, outer point the two paths stay
 * parallel and never cross.
 */
export const MARKS: readonly Mark[] = [
  { vx: 232, vy: 128, card: 'guardian', tone: '#ff4655' },
  { vx: 215, vy: 150, card: 'stakes', tone: '#d9954a' },
];
