import { KeyFigureIcon, KeyFigureTone } from '../key-figures/key-figures.model';
import { ConsistencyTrend } from './consistency.model';

/**
 * Width of one column of dots, in combat score.
 */
export const CONSISTENCY_BIN_WIDTH = 20;

/**
 * Step the axis ends are rounded to, and its tick spacing.
 */
export const CONSISTENCY_AXIS_STEP = 50;

/**
 * Lowest top of the vertical axis, so a season of short stacks does not blow its dots up.
 */
export const CONSISTENCY_MIN_STACK = 6;

/**
 * Tallest axis still graduated every two matches; above it the ticks go five by five.
 */
export const CONSISTENCY_SMALL_STACK = 10;

/**
 * Share of a column's width, or of a stack level's height, a dot's radius takes.
 */
export const CONSISTENCY_DOT_FILL = 0.42;

/**
 * Fill of a dot outside the floor-to-ceiling zone.
 */
export const CONSISTENCY_OUTSIDE_COLOR = 'rgb(236 232 225 / 0.22)';

/**
 * Stroke of the floor and ceiling rules.
 */
export const CONSISTENCY_RULE_COLOR = 'rgb(217 149 74 / 0.6)';

/**
 * Spread change, as a share of the previous one, under which seasons count as equally steady.
 */
export const CONSISTENCY_STEADY_MARGIN = 0.1;

/**
 * Pictogram of the trend figure, per trend.
 */
export const CONSISTENCY_TREND_ICONS: Readonly<Record<ConsistencyTrend, KeyFigureIcon>> = {
  tighter: 'tighter',
  looser: 'looser',
  steady: 'flat',
};

/**
 * Tone of the trend figure: a tighter spread is progress, a looser one a setback.
 */
export const CONSISTENCY_TREND_TONES: Readonly<Record<ConsistencyTrend, KeyFigureTone>> = {
  tighter: 'good',
  looser: 'bad',
  steady: 'neutral',
};

/**
 * Prefix of every translation key the consistency block reads.
 */
export const CONSISTENCY_I18N = 'playerProfile.progression.consistency';

/**
 * Tailwind classes of the outcome chip in the tooltip.
 */
export const CONSISTENCY_RESULT_CLASSES: Readonly<Record<string, string>> = {
  WIN: 'bg-success/14 text-success',
  LOSS: 'bg-danger/14 text-danger',
};

/**
 * Tailwind classes of the outcome chip for any other outcome.
 */
export const CONSISTENCY_RESULT_FALLBACK_CLASS = 'bg-text-primary/4 text-text-secondary';

/**
 * Matches with a combat score a season needs before the backend reports its spread.
 */
export const CONSISTENCY_MIN_SAMPLE = 8;
