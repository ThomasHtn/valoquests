import type { LucideIcon } from '@lucide/angular';
import {
  LucideArrowDownToLine,
  LucideArrowUpToLine,
  LucideChevronsDownUp,
  LucideChevronsUpDown,
  LucideGitCommitHorizontal,
  LucideMinus,
  LucideTrendingDown,
  LucideTrendingUp,
} from '@lucide/angular';

import { CONCEPT_ICONS } from '@core/concepts/concept.constants';
import { KeyFigureIcon, KeyFigureTone } from './key-figures.model';

/**
 * Icon tile modifier per tone.
 */
export const KEY_FIGURE_TONE_MODIFIERS: Readonly<Record<KeyFigureTone, string>> = {
  brand: 'key-figures__icon--brand',
  good: 'key-figures__icon--good',
  bad: 'key-figures__icon--bad',
  neutral: 'key-figures__icon--neutral',
};

/**
 * Lucide icon drawn for each pictogram; also the rank journey tooltip's delta arrow.
 */
export const KEY_FIGURE_ICONS: Readonly<Record<KeyFigureIcon, LucideIcon>> = {
  trendUp: LucideTrendingUp,
  trendDown: LucideTrendingDown,
  flat: LucideMinus,
  floor: LucideArrowDownToLine,
  median: LucideGitCommitHorizontal,
  ceiling: LucideArrowUpToLine,
  tighter: LucideChevronsDownUp,
  looser: LucideChevronsUpDown,
  matches: CONCEPT_ICONS.matches,
};
