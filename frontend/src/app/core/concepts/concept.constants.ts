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
import type { LucideIcon } from '@lucide/angular';

import { Concept } from './concept.model';

/**
 * The one icon of each concept (DESIGN §4): a wheat ear is food everywhere, a skull is the boss
 * everywhere. Read by `svg[lucideIcon]`, so a template never picks an icon for a concept by hand.
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

/**
 * The colour of the concepts that carry one, as text utilities; the others take their context's.
 */
export const CONCEPT_TONES: Readonly<Partial<Record<Concept, string>>> = {
  food: 'text-accent-green',
  components: 'text-accent-cyan',
  damage: 'text-boss-hp-edge',
  guardian: 'text-boss-hp-edge',
  wounded: 'text-brand-400',
  base: 'text-accent-gold',
};
