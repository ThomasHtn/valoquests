import { GAME_MODES } from './match-game-mode.constants';

/**
 * Game mode played during a match.
 */
export type GameMode = (typeof GAME_MODES)[number];
