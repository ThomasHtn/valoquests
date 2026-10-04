import type { LucideIcon } from '@lucide/angular';
import {
  LucideCalendar,
  LucideFlame,
  LucideGamepad2,
  LucideHeartPulse,
  LucideRocket,
  LucideSkull,
  LucideSwords,
  LucideTarget,
  LucideTrophy,
  LucideUsers,
  LucideWheat,
  LucideWrench,
  LucideZap,
} from '@lucide/angular';

import { Concept } from './concept.model';

/**
 * The one icon of each concept everywhere (DESIGN §4).
 */
export const CONCEPT_ICONS: Readonly<Record<Concept, LucideIcon>> = {
  food: LucideWheat,
  components: LucideWrench,
  damage: LucideSwords,
  guardian: LucideSkull,
  wounded: LucideHeartPulse,
  base: LucideUsers,
  challenge: LucideTarget,
  streak: LucideFlame,
  points: LucideZap,
  rocket: LucideRocket,
  day: LucideCalendar,
  matches: LucideGamepad2,
  score: LucideTrophy,
};
