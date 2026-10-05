import { MatchStatTone } from './match-history.model';

/**
 * Modifier colouring a stat figure, by tone.
 */
export const STAT_TONE_CLASSES: Readonly<Record<MatchStatTone, string>> = {
  primary: '',
  muted: 'match-history__stat-value--muted',
  good: 'match-history__stat-value--good',
  average: 'match-history__stat-value--average',
};
