/**
 * Weekly title; mirrors the backend `WeeklyTitle`. Ties award nothing.
 * Regular: days played, Scout: challenges, Quartermaster: food, Mechanic: components.
 */
export type WeeklyTitle = 'REGULAR' | 'SCOUT' | 'QUARTERMASTER' | 'MECHANIC';

/**
 * Champion or one of the weekly titles.
 */
export type TitleKey = 'CHAMPION' | WeeklyTitle;
