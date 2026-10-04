import { ConfettiPiece } from './progress-mark.model';

/**
 * Confetti burst of a landing check, jittered; sideways reach stays inside a 5.25rem column.
 */
export const CONFETTI: readonly ConfettiPiece[] = [
  { angle: -95, distance: 3.4, spin: 220, strip: true, pale: false },
  { angle: -65, distance: 2.6, spin: -160, strip: false, pale: true },
  { angle: -35, distance: 2.6, spin: 280, strip: true, pale: true },
  { angle: -10, distance: 2.3, spin: -240, strip: false, pale: false },
  { angle: 20, distance: 2.3, spin: 180, strip: true, pale: false },
  { angle: 50, distance: 2.2, spin: -200, strip: false, pale: true },
  { angle: 80, distance: 2.9, spin: 260, strip: true, pale: false },
  { angle: 110, distance: 2.4, spin: -180, strip: false, pale: false },
  { angle: 140, distance: 3, spin: 240, strip: true, pale: true },
  { angle: 170, distance: 2.3, spin: -260, strip: false, pale: false },
  { angle: 200, distance: 2.3, spin: 200, strip: true, pale: false },
  { angle: 230, distance: 3.2, spin: -220, strip: false, pale: true },
  { angle: 255, distance: 2.5, spin: 300, strip: true, pale: false },
];
