import { ColonyPresenceState, ColonyTierState, ColonyWeekOutcomeState } from './colony.model';
import { ColonyTrack } from './colony-gauge.utils';

/**
 * The glyph seated in a rail's hexagonal socket.
 *
 * Morale carries a face rather than a heart or a banner: a heart is hit points, which is exactly the
 * bar this replaced, and a banner does not say whether the morale is good. The three faces are set at
 * what a single fight moves in a week, so the glyph changes about as often as the fight does.
 */
export type ColonyTrackGlyph =
  'FOOD' | 'PRESENCE' | 'MORALE_GOOD' | 'MORALE_NEUTRAL' | 'MORALE_BAD';

/**
 * One swatch of a rail's hover card: a mark in the band's own colour, and what that band is worth.
 *
 * Each swatch is worth <b>the width of its own band</b>, never the point where it ends. Writing the
 * whole stock against the bright band, which is only the surplus, gave a reader who lined the two up
 * a number that was not the one they were looking at.
 */
export interface ColonyTrackLegendView {
  readonly colorClass: string;
  readonly label: string;
}

/**
 * One rail of the resource band, resolved into everything the page lays out.
 *
 * The band draws three rails and writes no label on any of them: the glyph in the socket names the
 * rail, and the figure and subtitle on its trailing edge say what it is worth. `label` stays in the
 * markup for assistive technology, which has neither a glyph nor a hover.
 */
export interface ColonyTrackView {
  readonly track: ColonyTrack;
  readonly glyph: ColonyTrackGlyph;

  /**
   * Already-translated rail name, for assistive technology only.
   */
  readonly label: string;

  /**
   * Where the first band ends: what the town already eats, or the morale's unreachable floor.
   */
  readonly percentage: number;

  /**
   * Where the second band ends, or `null` on a single-band rail.
   */
  readonly secondaryPercentage: number | null;

  /**
   * The fact, on the rail's trailing edge. One number on the food rail: what this week's food can
   * feed, which is the only ceiling the town has.
   */
  readonly valueLabel: string;

  /**
   * Already-translated sentence the rail's hover card shows, and the button's own accessible name —
   * so nothing on the band exists only on hover.
   */
  readonly ariaLabel: string;

  /**
   * Swatches the rail's hover card lists, empty on a rail whose card shows something else.
   */
  readonly legend: readonly ColonyTrackLegendView[];

  /**
   * One already-translated line closing that card, empty when there is nothing to add.
   */
  readonly note: string;

  /**
   * Tailwind background utility of the rim around the rail's glyph socket.
   *
   * The rail's own colour at full strength, on all three rails, so the three sockets read as one
   * family. Deliberately not the first band's utility: that one is muted on two rails out of three,
   * which drew a bright cyan rim beside two washed-out ones.
   */
  readonly socketClass: string;

  /**
   * Tailwind background utility of the first band.
   */
  readonly primaryClass: string;

  /**
   * Tailwind background utility of the second band, empty on a single-band rail.
   */
  readonly secondaryClass: string;

  readonly textClass: string;
}

/**
 * One pip of the turnout card: a player of the roster, lit by how far into the day they got.
 */
export interface ColonyPresencePipView {
  readonly playerId: number;
  readonly name: string;
  readonly state: ColonyPresenceState;

  /**
   * Two or three letters under the pip. Enough to recognise a squad of seven, short enough that
   * seven of them fit on one line.
   */
  readonly initials: string;

  /**
   * Tailwind background utility: lit, half-lit, or the empty track's own colour.
   */
  readonly fillClass: string;

  /**
   * Already-translated name and state, since the pip itself carries neither in text.
   */
  readonly ariaLabel: string;
}

/**
 * One step of the ladder, resolved into everything the panel lays out.
 */
export interface ColonyTierStepView {
  readonly threshold: number;
  readonly state: ColonyTierState;

  /**
   * Already-translated step name, citadel number included.
   */
  readonly name: string;

  /**
   * The figure beside the name. On the step the town sits in this is what it is climbing
   * <b>towards</b>, not where it stands: read at the top of a row, in the row's own weight, the
   * town's efficiency was taken for the target, which is the one thing it is not. Where it stands
   * is on the bar's own tooltip instead, in {@link progressLabel}.
   */
  readonly valueLabel: string;

  /**
   * Progress towards the next step, or `null` on every step but the town's own.
   */
  readonly progressPercentage: number | null;

  /**
   * Already-translated sentence the bar hands to its tooltip: where the town stands, what the step
   * it is climbing runs to, and what is left to cross it. Empty off the active step.
   *
   * In a tooltip rather than under the bar: a second row of figures under the active step was read
   * as the step's own target, which is what the figure beside the name already is. The bar carries
   * the position, and only says it in figures when asked.
   */
  readonly progressLabel: string;
}

/**
 * One of the run's ten fights, resolved into everything the map lays out.
 */
export interface ColonyBossView {
  /**
   * Week of the run, from one.
   */
  readonly weekIndex: number;
  readonly state: ColonyWeekOutcomeState;

  /**
   * Already-formatted efficiency the fight is worth, written on the week's own territory: `+0,53`
   * when it was taken, `0` when the boss held, empty on a week not yet reached.
   *
   * Efficiency rather than materials or morale. Materials are an intermediate currency the player
   * never handles, efficiency is the axis the whole page already counts in, and it is the only part
   * of a fight's reward still standing on settlement day.
   */
  readonly efficiencyLabel: string;

  /**
   * Whether that efficiency is banked or still on the table.
   */
  readonly efficiencyEarned: boolean;

  /**
   * The full sentence the tile's title carries: the week, the outcome, its materials, the efficiency
   * they buy and the morale it moved. Morale does not fit on a tile and lives here instead.
   */
  readonly detailLabel: string;
}

/**
 * One run of the history table, resolved into everything the page lays out.
 */
export interface ColonyRunView {
  readonly runNumber: number;

  /**
   * Already-translated run name, `Run 4`.
   */
  readonly label: string;

  /**
   * Whether this is the run in progress, whose figures are still moving.
   */
  readonly isCurrent: boolean;

  /**
   * Already-formatted final population, the run's score.
   */
  readonly finalLabel: string;
}

/**
 * What the night moved, and which way.
 *
 * Carried as a pair rather than as a signed string alone: the figure is coloured by its direction,
 * and a template deriving that from a leading `-` would break the moment the language changes its
 * minus sign.
 */
export interface ColonyDeltaView {
  /**
   * Already-formatted movement, sign always shown.
   */
  readonly label: string;

  /**
   * Whether the town grew. A night that moved nobody counts as neither, and reads muted.
   */
  readonly isPositive: boolean;
  readonly isNegative: boolean;
}

/**
 * How far into the run today is, kept in parts so the context bar can gild the day alone.
 */
export interface RunDayParts {
  /**
   * Already-translated word opening the label, `Day`.
   */
  readonly word: string;

  readonly day: number;
  readonly days: number;

  /**
   * Already-translated bubble saying how long the campaign runs.
   */
  readonly hint: string;
}
