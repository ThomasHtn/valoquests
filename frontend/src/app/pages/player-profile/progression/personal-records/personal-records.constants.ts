import type { LucideIcon } from '@lucide/angular';
import {
  LucideBomb,
  LucideCalendarCheck,
  LucideCrosshair,
  LucideGauge,
  LucideLocateFixed,
  LucideMedal,
  LucideShieldCheck,
  LucideStar,
  LucideTrendingUp,
} from '@lucide/angular';

import { RecordKey } from './personal-records.model';

/**
 * Icon of each record; the peak tier draws its rank badge instead, the shield is its type filler.
 */
export const RECORD_ICONS: Readonly<Record<RecordKey, LucideIcon>> = {
  mostKills: LucideCrosshair,
  bestAcs: LucideGauge,
  mostDamage: LucideBomb,
  bestKda: LucideMedal,
  bestHeadshotPercentage: LucideLocateFixed,
  longestWinStreak: LucideTrendingUp,
  longestActiveDayStreak: LucideCalendarCheck,
  mvps: LucideStar,
  peakTier: LucideShieldCheck,
};
