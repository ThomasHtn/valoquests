import {
  ColonyPresenceState,
  ColonyTierName,
  ColonyTierState,
  ColonyWeekOutcomeState,
} from './colony.model';

/**
 * The silhouette one step of the ladder wears.
 *
 * Four bands rather than one glyph per name: twelve icons in a panel this narrow would each be read
 * as a different kind of thing, where the point is that they are one thing growing. The bands are the
 * ladder's own arc — a camp, then houses, then a skyline, then a monument.
 */
export type ColonyTierGlyph = 'CAMP' | 'HOUSES' | 'SKYLINE' | 'MONUMENT' | 'SPRAWL';

/**
 * The town, drawn as the one thing the game is actually about — see design-review.md §3.1.
 *
 * A single silhouette rather than the ladder's twelve rows: the band the town's current step
 * belongs to picks *which* silhouette, and the two percentages are all that silhouette needs to
 * read as alive — how many of its windows are lit, and how far the scaffold on its tallest
 * building has climbed towards the next step.
 */
export interface ColonyTownView {
  /**
   * The step the colony currently stands on, which is what the scene builds itself from: every
   * named step of the ladder adds something to the waterfront (see `town-scene.ts`).
   */
  readonly tier: ColonyTierName;

  /**
   * The same step as a position on the ladder, from zero and open-ended — `tierStepFor` folds the
   * citadel number back in. The scene reads this rather than {@link tier}: the twelfth name repeats,
   * so a drawing keyed on the name stops moving for the last third of a strong run.
   */
  readonly step: number;

  /**
   * Which of the four silhouettes the ladder's own marker draws — the current step's band.
   */
  readonly glyph: ColonyTierGlyph;

  /**
   * Share of the silhouette's windows lit, `0`–`100` — population over what the town's food can
   * feed, the same figure the hexagon elsewhere on the page fills on.
   */
  readonly populationPercentage: number;

  /**
   * Share of the way to the next step, `0`–`100`, drawn as how much of the quarter under scaffolding
   * has been built. The ladder repeats past its named steps (see {@link ColonyTierGlyph}), so there
   * is always a next step to climb towards, and therefore always a site standing in the scene.
   */
  readonly progressPercentage: number;

  /**
   * Share of the roster that played today, `0`–`100`, which lights the street and fills the
   * crossing.
   *
   * The scene's only genuinely *daily* figure. The step moves about once a week and the lit windows
   * move by a percent a day, so without this the drawing is the same drawing from one Monday to the
   * next, which is the whole of what it was reproached for.
   */
  readonly turnoutPercentage: number;
}

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
 * The turnout rail, redrawn as a continuous charge rather than one cell per roster member — a
 * fixed-length array of cells reads as a list, and a list is exactly what stops being legible past
 * a couple dozen players (see the root `CLAUDE.md` on the squad being a variable, not a constant).
 *
 * Read as a level rather than a count on purpose — the same reason a fuel gauge outsells a litre
 * counter: what a reader needs to decide whether tonight is worth playing is "how full", not the
 * two figures a fraction makes them subtract themselves.
 */
export interface ColonyBatteryView {
  /**
   * Roster size the run is frozen on — the charge's own denominator.
   */
  readonly rosterSize: number;

  /**
   * Players who cleared the threshold today, out of {@link rosterSize}.
   */
  readonly presentCount: number;

  /**
   * `presentCount` over `rosterSize`, as a `0`–`100` fill — the bar's own width, independent of the
   * roster size so it never grows a segment per player.
   */
  readonly presentPercentage: number;

  /**
   * Where one more player would land on the bar, `0`–`100`. `null` once the roster is already full:
   * there is no next step left to mark.
   */
  readonly nextStepPercentage: number | null;

  /**
   * Whether every cell is lit, which is what earns the battery its glow.
   */
  readonly isFull: boolean;

  /**
   * Already-formatted factor the charge is worth on tonight's harvest, `×1,43`.
   */
  readonly multiplierLabel: string;

  /**
   * Already-formatted factor one more player would be worth, `×1,60`. `null` once the roster is
   * already full — read as "no next step to reach", same as {@link nextStepPercentage}.
   */
  readonly nextMultiplierLabel: string | null;

  /**
   * Already-formatted factor a full roster is worth, `×2,00` — the ceiling {@link multiplierLabel}
   * is read against, so a bare `×1,17` is not left to be judged against a scale nobody stated.
   */
  readonly maxMultiplierLabel: string;

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
   * Share of the week's spoken-for stock (consumption + surplus) already eaten, `0`–`100` — the
   * accueil's own bar reads the split as a threshold ("does the harvest cover what is eaten?")
   * rather than three figures to add up, per design-review.md §3.1.
   */
  readonly consumptionPercentage: number;

  /**
   * Where the spoken-for stock ends, `0`–`100`. Always `100` once there is any stock at all: by
   * definition, consumption plus surplus accounts for the whole of it — `0` only while the run has
   * not banked any food yet.
   */
  readonly surplusPercentage: number;

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
 * {@link ColonyPresencePipView}, bounded to a fixed count regardless of roster size.
 *
 * The rule this exists for (root `CLAUDE.md`, § "the squad is a variable"): a measure never
 * degrades, but a per-player list does — one row per player is fine at seven, unreadable at fifty.
 * `visible` wraps onto two rows at the card's usual width and never grows past {@link overflowCount}
 * kicking in, so the face band stays a detail under the presence bar rather than a second measure.
 */
export interface ColonyPresenceFacesView {
  /**
   * Pips shown as faces, capped regardless of roster size.
   */
  readonly visible: readonly ColonyPresencePipView[];

  /**
   * Roster members left out of {@link visible}, `0` when the whole roster fit.
   */
  readonly overflowCount: number;
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
   * {@link missingMaterialsLabel} as the bare number it is, for a caller comparing it against a
   * projected total (e.g. "would this week's perfect haul reach the next tier?") rather than only
   * printing it.
   */
  readonly missingMaterials: number;

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
 * The step of the ladder this week's fight buys, resolved for the week page's payout block.
 *
 * A bare `+480 matériaux` states an amount and no scale: nothing on the page says whether that is a
 * good week or a wasted one. Read against the step it is paying for, the same amount says how much
 * of the climb one Sunday covers, which is the only form of it a reader can act on.
 */
export interface ColonyBossPayoutStepView {
  /**
   * Already-translated names of the step the colony stands on and the one it is climbing to.
   */
  readonly fromName: string;
  readonly toName: string;

  /**
   * The silhouettes those two steps wear, so the trail here is drawn with the same marks the
   * ladder strip uses on the accueil.
   */
  readonly fromGlyph: ColonyTierGlyph;
  readonly toGlyph: ColonyTierGlyph;

  /**
   * Share of the step already climbed, in `[0, 100]` — the colony's own `tierProgressPercentage`.
   */
  readonly havePercentage: number;

  /**
   * Share this fight would add, clamped to what is left of the track so the band never runs past
   * its own rail. {@link gainLabel} states the true figure, which can exceed what is left.
   */
  readonly gainPercentage: number;

  /**
   * That same share, already formatted and signed (`+71 %`).
   */
  readonly gainLabel: string;
}

/**
 * What the week's fight pays the colony, resolved for the week page.
 */
export interface ColonyBossPayoutView {
  /**
   * Already-formatted materials a defeated boss banks, and the morale it adds.
   */
  readonly materialsLabel: string;
  readonly moraleLabel: string;

  /**
   * Already-formatted morale a boss left standing costs, signed negative.
   */
  readonly defeatMoraleLabel: string;

  /**
   * The step those materials buy, or `null` on a run with no fight under way or a boss worth no
   * efficiency at all — there is then no climb to draw, only the two figures above.
   */
  readonly step: ColonyBossPayoutStepView | null;
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
   * {@link materialsLabel} as the bare number it is, `0` wherever the label is empty — for a caller
   * summing it with other figures (e.g. "the week, played perfectly") rather than only printing it.
   */
  readonly materialsValue: number;

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
  /**
   * Stable identity for the `@for` track expression — never displayed (design-review.md's
   * validated mock-up drops the run number from public view in favor of its dates, see
   * {@link dateRangeLabel}).
   */
  readonly runNumber: number;

  /**
   * Whether this is the run in progress, whose figures are still moving.
   */
  readonly isCurrent: boolean;

  /**
   * Already-formatted `firstDay - settlementDay` span, e.g. `"24/08 - 01/11"` — what a run is now
   * identified by instead of its number.
   */
  readonly dateRangeLabel: string;

  /**
   * Already-translated week progress, `"Sem. 4 / 10"` for the run in progress, `"Sem. 10 / 10"`
   * for a closed one (every closed run today ran its full ten weeks — there is no early stop yet).
   */
  readonly weekLabel: string;

  /**
   * Already-translated status, `"En cours"` / `"Terminée"`.
   */
  readonly statusLabel: string;

  /**
   * Already-formatted final population, the run's score.
   */
  readonly finalLabel: string;

  /**
   * Threats the run put down, over the ten it holds (`2 / 10`) — the column that says how a
   * campaign was played rather than only where it landed.
   */
  readonly bossesLabel: string;

  /**
   * Already-translated name of the step the town reached, citadel number included.
   */
  readonly tierLabel: string;
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
