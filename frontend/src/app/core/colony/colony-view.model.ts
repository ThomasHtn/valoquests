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
 * The silhouette one step of the ladder wears.
 *
 * Four bands rather than one glyph per name: twelve icons in a panel this narrow would each be read
 * as a different kind of thing, where the point is that they are one thing growing. The bands are the
 * ladder's own arc — a camp, then houses, then a skyline, then a monument.
 */
export type ColonyTierGlyph = 'CAMP' | 'HOUSES' | 'SKYLINE' | 'MONUMENT';

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
 * One named figure of a rail's hover card: what it is, and what it is worth.
 *
 * Where a rail's track carries a shape a reader can only estimate, this carries the numbers behind
 * it. Food uses it for the three figures its pills cannot state — what one point of food is worth in
 * inhabitants, what the town needs, and what is left over — which is the whole of what the card is
 * for now that the week itself is on the rail.
 */
export interface ColonyTrackFactView {
  readonly label: string;
  readonly value: string;
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
   * Whether the rail draws a bar at all.
   *
   * False on food alone. A bar states a level against a ceiling, and the food stock is a rolling
   * seven-day sum with no ceiling to state it against — every denominator tried was either the
   * population figure seen a second time or a number invented for the occasion. That rail draws its
   * window day by day instead, which is what the stock actually is.
   */
  readonly hasBar: boolean;

  /**
   * Where the fill ends: the turnout out of the frozen roster, or the morale out of its ceiling.
   * Ignored off a rail with a bar.
   */
  readonly percentage: number;

  /**
   * Where a second band ends, or `null` on a single-band rail, which all three currently are.
   */
  readonly secondaryPercentage: number | null;

  /**
   * Whether the fill is a value still moving, which is what earns it the travelling highlight.
   *
   * False on food alone, which draws no bar at all.
   */
  readonly isLive: boolean;

  /**
   * The fact, on the rail's trailing edge.
   *
   * A part of a whole where the rail has one — `5 / 7` turnout, `55 / 100` morale — and a plain
   * total where it does not: food is a rolling seven-day sum with no ceiling, so its rail carries
   * the stock itself. The fraction form is reserved for genuine parts of a whole. Food briefly wrote
   * `surplus / consumption` in it, and a reader who knew the other two rails read a stock not quite
   * half full when neither figure was the whole.
   */
  readonly valueLabel: string;

  /**
   * What the night takes off that fact, under it, or empty when nothing is coming off.
   *
   * Only food has one: its oldest day expires tonight, and that is the single thing about a rolling
   * window a reader has to know before deciding whether to play.
   */
  readonly valueDeltaLabel: string;

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
   * Named figures the rail's hover card lists, empty on a rail whose card shows something else.
   */
  readonly facts: readonly ColonyTrackFactView[];

  /**
   * Already-translated sentence opening the card: how this resource is obtained.
   */
  readonly description: string;

  /**
   * Already-translated sentence closing the card: what this resource is for.
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
 * One day of the food week strip: a harvest, drawn as a share of the best day of the window.
 *
 * The food rail's own track, since that rail has no bar — the counterpart of
 * {@link ColonyPresencePipView}, which is the other rail whose card the band fills itself. The stock
 * is a rolling seven-day window rather than a reserve, so these seven pills are the honest picture
 * of it: a total says how much food there is, the pills say which evenings put it there and which
 * one is about to be forgotten.
 *
 * Each pill fills along its own length rather than rising in place. The rail is fourteen pixels
 * tall and around seventy wide per day, so height gave an evening worth a fifth of the week under
 * three pixels — a magnitude nobody could read, and one that could not even be told apart from an
 * evening nobody played.
 *
 * There is one pill per day the window holds, which is fewer than seven only while a young run's
 * window is still filling. No hollow pill ever stands in for a day before the run began: the strip
 * is laid out `flex-1`, so four pills fill the rail exactly as seven do.
 */
export interface ColonyFoodDayView {
  /**
   * ISO day, and the `@for` track expression's identity.
   */
  readonly day: string;

  /**
   * How far the pill fills, as a share of the window's best day, in `[0, 100]`.
   */
  readonly percentage: number;

  /**
   * Whether this is the day being lived, which the strip marks so the row can be read from it.
   */
  readonly isToday: boolean;

  /**
   * Whether tonight drops this day out of the window.
   *
   * Never true while the window is still filling, where the run has simply not lived seven days yet
   * and nothing is being lost.
   */
  readonly isExpiring: boolean;

  /**
   * Already-translated day and harvest, since the column itself carries neither in text.
   */
  readonly ariaLabel: string;
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

  /**
   * What the step still costs, already formatted: the materials to bank before it opens.
   *
   * The column used to carry the step's efficiency, a bare `8,75` that names no unit, appears nowhere
   * else on the page and that no action moves directly. The materials still missing are the same
   * threshold said in the currency challenges and bosses actually pay. A step already paid reads
   * `0` rather than blank, so the town's own step is visibly settled instead of merely silent.
   */
  readonly missingMaterialsLabel: string;

  /**
   * Which silhouette the step's marker wears, so the ladder reads as a settlement growing rather than
   * as a list of names. The step's *state* is carried by the marker's colour and fill, which is why
   * the icon is free to carry the step itself.
   */
  readonly glyph: ColonyTierGlyph;
  readonly state: ColonyTierState;

  /**
   * Whether this is the step the town is climbing towards, one above its own.
   *
   * Drawn as the panel's active row rather than the town's own step: the step below is already paid,
   * and the row a reader has to act on is the one still to open.
   */
  readonly isNext: boolean;

  /**
   * Already-translated step name, citadel number included.
   */
  readonly name: string;

  /**
   * How far the town has climbed towards this step, or `null` on every step but the next one.
   */
  readonly progressPercentage: number | null;

  /**
   * Already-translated sentence the bar hands to its tooltip: what the step still costs to open.
   * Empty off the step being climbed.
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
   * Already-formatted materials the fight is worth, written on the week's own territory: `560` when
   * it was taken or is still on the table, empty on a week whose boss held.
   *
   * Materials rather than the efficiency they buy, which is what the tile used to carry. Efficiency
   * is a figure nothing else on the page counts in and that the squad cannot act on; materials are
   * the unit the ladder is priced in and the one challenges pay, so the map, the ladder and the
   * header now read as one currency.
   */
  readonly materialsLabel: string;

  /**
   * Already-formatted efficiency those materials add to the town's rate (`+0,27`), never a factor:
   * empty wherever `materialsLabel` is, since there is nothing to add on a week that settled nothing.
   *
   * Lives in the hover card only, beside the materials figure it explains — the tile itself stays
   * priced in materials alone, the one currency the map and the ladder share.
   */
  readonly efficiencyLabel: string;

  /**
   * Whether those materials are banked, as opposed to still on the table.
   */
  readonly earned: boolean;

  /**
   * Already-formatted morale the fight moved, signed.
   */
  readonly moraleLabel: string;
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
