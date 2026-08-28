import { ColonyPresenceState, ColonyTierState, ColonyWeekOutcomeState } from './colony.model';

/**
 * The silhouette one step of the ladder wears.
 *
 * Four bands rather than one glyph per name: twelve icons in a panel this narrow would each be read
 * as a different kind of thing, where the point is that they are one thing growing. The bands are the
 * ladder's own arc — a camp, then houses, then a skyline, then a monument.
 */
export type ColonyTierGlyph = 'CAMP' | 'HOUSES' | 'SKYLINE' | 'MONUMENT';

/**
 * One day of the food week ring: a harvest, drawn as a share of the best day of the window.
 *
 * The stock is a rolling seven-day window rather than a reserve, so these seven pods are the
 * honest picture of it: a total says how much food there is, the pods say which evenings put it
 * there. The ring always draws a fixed `foodWindowDays` slots, newest first from the top going
 * clockwise, so every pod keeps the same size around the circle: a young run's window, still
 * filling, pads the remainder with {@link isPlaceholder} slots rather than stretching what it has
 * around the whole circle.
 */
export interface ColonyFoodDayView {
  /**
   * ISO day, or a synthetic key for a placeholder slot — the `@for` track expression's identity.
   */
  readonly day: string;

  /**
   * How far the pod fills, as a share of the window's best day, in `[0, 100]`.
   */
  readonly percentage: number;

  /**
   * Whether this is the most recent day of the window — the one still open, closing at tonight's
   * reset — which the ring colours to mark it. Set from the day's position in the window, never
   * from the calendar: what the colour says is "tonight's rollover empties this one", not "this is
   * today".
   */
  readonly isLast: boolean;

  /**
   * Whether this is the day actually being lived, by the calendar rather than by position. Never
   * true for a placeholder. Marked with a live shimmer rather than a colour of its own, since it can
   * land on the same segment {@link isLast} already colours, or, once the window is full and today's
   * harvest has not posted yet, on a different one entirely.
   */
  readonly isToday: boolean;

  /**
   * Whether this slot pads a window still filling: a day the run has not lived yet, rather than one
   * it lived without playing.
   */
  readonly isPlaceholder: boolean;

  /**
   * Colour the ring's conic-gradient draws this day's segment in, already resolved from
   * {@link isLast}, {@link isPlaceholder} and {@link percentage} — see `colony-gauge.utils`.
   */
  readonly segmentColor: string;

  /**
   * Already-translated day and harvest, since the pod itself carries neither in text. Empty on a
   * placeholder slot, which has nothing to say.
   */
  readonly ariaLabel: string;

  /**
   * Already-formatted harvest of this day, for the food-this-week tile's tooltip. Empty on a
   * placeholder slot.
   */
  readonly harvestLabel: string;

  /**
   * Raw harvest of this day, for the food-this-week line's own point. `null` on a placeholder
   * slot, so the line stops rather than dipping to zero for a day not lived yet.
   */
  readonly harvestValue: number | null;

  /**
   * First letter of the day's name, already translated, for the food-this-week line's own axis.
   * Empty on a placeholder slot.
   */
  readonly weekdayInitial: string;
}

/**
 * The turnout rail, redrawn as a charge rather than a fraction: one cell of the battery per player
 * the roster is frozen on, lit bottom-up by how many turned up tonight.
 *
 * Read as a level rather than a count on purpose — the same reason a fuel gauge outsells a litre
 * counter: what a reader needs to decide whether tonight is worth playing is "how full", not the
 * two figures a fraction makes them subtract themselves.
 */
export interface ColonyBatteryView {
  /**
   * Cells the battery is built from — the roster size frozen on the run.
   */
  readonly cellCount: number;

  /**
   * One flag per cell, lit from index zero: `cells[i]` is lit when `i` is under tonight's turnout.
   *
   * Resolved here rather than left to the template, which has no clean way to build a fixed-length
   * array from a count alone.
   */
  readonly cells: readonly boolean[];

  /**
   * Whether every cell is lit, which is what earns the battery its glow.
   */
  readonly isFull: boolean;

  /**
   * Cells lit by tonight's turnout, out of {@link cellCount} — the Participation tile's own
   * headline count, read beside the roster size rather than only as a level.
   */
  readonly presentCount: number;

  /**
   * Already-formatted factor the charge is worth on tonight's harvest, `×1,43`.
   */
  readonly multiplierLabel: string;

  /**
   * Already-translated sentence the battery's hover card shows, and its own accessible name.
   */
  readonly ariaLabel: string;

  /**
   * Already-translated sentence opening the card: how the charge is built.
   */
  readonly description: string;

  /**
   * Already-translated sentence closing the card: what the charge is spent on.
   */
  readonly purpose: string;
}

/**
 * The morale rail: what it is, read as a fill against its own ceiling — what it buys is the
 * band's own arrival mark, drawn once the food dome and this one are both on the page.
 */
export interface ColonyAttractivityView {
  /**
   * Where the bar's fill ends: morale itself, out of its ceiling.
   */
  readonly percentage: number;

  /**
   * Already-formatted morale itself, `50 / 100` — what the hover card and assistive technology
   * read the bar's fill as.
   */
  readonly moraleLabel: string;

  /**
   * Morale alone, `50` — the dome's own headline, read in its hollow against the fill already
   * drawing it out of its ceiling, which is why the ceiling is not repeated in text.
   */
  readonly moraleValueLabel: string;

  /**
   * Already-translated sentence the bar's hover card shows, and its own accessible name.
   */
  readonly ariaLabel: string;

  /**
   * Already-translated sentence opening the card: how morale moves.
   */
  readonly description: string;

  /**
   * Already-translated sentence closing the card: what morale buys.
   */
  readonly purpose: string;
}

/**
 * The food rail, redrawn as a ring: the week's seven evenings arranged around the stock they add
 * up to, and the stock itself read as the small sum it actually is — a harvest, a consumption, and
 * what is left once the town has eaten.
 */
export interface ColonyFoodRingView {
  /**
   * The window's own days, oldest first — the ring's own track, positioned by the template.
   */
  readonly days: readonly ColonyFoodDayView[];

  /**
   * Already-formatted weekly consumption, what the town eats regardless of what it plays.
   */
  readonly consumptionLabel: string;

  /**
   * Already-formatted, signed weekly surplus — the ring centre's one bold figure.
   */
  readonly surplusLabel: string;

  /**
   * Already-formatted efficiency, for the hover card alone: what one point of stock is worth.
   */
  readonly efficiencyLabel: string;

  /**
   * Efficiency as the bare factor it is, `×8,00` — read beside the dome's own title, since it is
   * what materials from challenges and bosses buy and is otherwise not shown anywhere the page
   * does not need a hover to reach.
   */
  readonly efficiencyFactorLabel: string;

  /**
   * Already-translated sentence the ring's hover card shows, and its own accessible name.
   */
  readonly ariaLabel: string;

  /**
   * Already-translated sentence opening the card: how food is harvested.
   */
  readonly description: string;

  /**
   * Already-translated sentence closing the card: what food is for.
   */
  readonly purpose: string;
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
