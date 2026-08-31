/**
 * The names a town wears as it grows, from the smallest to the largest.
 *
 * Mirrors the backend `ColonyTierName`. Purely decorative: crossing a step changes no rule, it only
 * gives the run a milestone roughly once a week. `STRATUM` is the open end of the ladder — past it
 * every further step is a numbered stratum, which is what lets the names run on without a maximum.
 *
 * The names run to the seventeenth step because that is where the barème says a squad can get: a run
 * at its calibrated pace ends around `CITADEL`, one clearing nearly every challenge reaches step
 * seventeen. Every step reachable has a name, and the numbering is the ceiling's ceiling.
 */
export type ColonyTierName =
  | 'CAMP'
  | 'HAMLET'
  | 'VILLAGE'
  | 'BOROUGH'
  | 'TOWN'
  | 'CITY'
  | 'RESIDENTIAL_QUARTER'
  | 'GREAT_CITY'
  | 'METROPOLIS'
  | 'MEGALOPOLIS'
  | 'CAPITAL'
  | 'CITADEL'
  | 'CONURBATION'
  | 'MEGAREGION'
  | 'ARCOLOGY'
  | 'ECUMENOPOLIS'
  | 'CONTINUUM'
  | 'STRATUM';

/**
 * Where one step of the ladder stands relative to the town.
 *
 * Mirrors the backend `ColonyTierState`.
 */
export type ColonyTierState = 'REACHED' | 'CURRENT' | 'LOCKED';

/**
 * How far into the day one player of the roster got.
 *
 * Mirrors the backend `ColonyPresenceState`. Three states rather than two: an evening of two
 * deathmatches brings food in and still does not count towards the multiplier, and drawing that as
 * "did not play" would be false.
 */
export type ColonyPresenceState = 'FULL' | 'PARTIAL' | 'NONE';

/**
 * How one week of the run's ten fights ended.
 *
 * Mirrors the backend `ColonyWeekOutcomeState`.
 */
export type ColonyWeekOutcomeState = 'DEFEATED' | 'SURVIVED' | 'CURRENT' | 'UPCOMING';

/**
 * The category a week's boss was drawn at.
 *
 * Mirrors the backend `BossCategory`.
 */
export type ColonyBossCategory = 'MINOR' | 'STANDARD' | 'ELITE';

/**
 * One step of the town's ladder.
 *
 * Mirrors `ColonyTierResponse`.
 */
export interface ColonyTier {
  readonly name: ColonyTierName;

  /**
   * Citadel number once the ladder starts repeating, zero on every named step.
   */
  readonly level: number;

  readonly threshold: number;

  /**
   * Cumulative materials the run's roster must bank to open this step.
   *
   * The same threshold, priced in the one currency the squad can act on. An efficiency says nothing
   * about what to do tonight; a materials figure points straight at the challenges and the boss.
   */
  readonly materialsRequired: number;

  readonly state: ColonyTierState;
}

/**
 * One day of the seven-day harvest the food stock is made of.
 *
 * Mirrors `ColonyFoodDayResponse`. The stock is a rolling window, never a reserve, so these seven days
 * are the only reading of the food that says *when* the squad played — and the only one that is not
 * the population figure seen a second time.
 */
export interface ColonyFoodDay {
  readonly day: string;
  readonly harvest: number;
}

/**
 * One player of the roster and how far into today they got.
 *
 * Mirrors `ColonyPresencePlayerResponse`.
 */
export interface ColonyPresencePlayer {
  readonly playerId: number;
  readonly name: string;
  readonly state: ColonyPresenceState;

  /**
   * Raw damage brought in today, before the daily diminishing returns — the figure the threshold is
   * read on.
   */
  readonly rawDamage: number;
}

/**
 * The day's turnout and the multiplier it is worth.
 *
 * Mirrors `ColonyPresenceResponse`. The multiplier is divided by the roster frozen on the run, never
 * by a hard seven, so a five-player squad reaches a full house with five.
 */
export interface ColonyPresence {
  readonly present: number;
  readonly rosterSize: number;
  readonly multiplier: number;

  /**
   * What the multiplier would be with one more player counted, capped at the roster size. Equal to
   * {@link multiplier} once the roster is full — a UI reading it should treat equality as "no next
   * step to reach" rather than compute the next step itself.
   */
  readonly nextMultiplier: number;

  readonly threshold: number;
  readonly players: readonly ColonyPresencePlayer[];
}

/**
 * The morale, which is the speed the town moves at and nothing else.
 *
 * Mirrors `ColonyMoraleResponse`. Only the weekly boss moves it, and it applies on the way up alone:
 * a demoralised town falls exactly as fast as any other.
 */
export interface ColonyMorale {
  readonly value: number;
  readonly ceiling: number;

  /**
   * Share of the gap tonight closes at this morale, which is the whole of what morale does.
   *
   * Drawn on the rail beside the value, in the slot turnout uses for its own multiplier: the two are
   * both factors on what an evening is worth, and morale's was the one left unsaid.
   */
  readonly growthPercentPerNight: number;
}

/**
 * What one week of the run's fights was worth to the colony.
 *
 * Mirrors `ColonyWeekResponse`. Priced in materials, which is the one currency the map, the ladder and
 * the hover card all read in: a tile's figure can then be compared straight against the step of the
 * ladder it helps pay for. `efficiencyGain` is that same reward read the other way, as the fraction of
 * an efficiency point it buys back, for the hover card that already carries the materials figure and can
 * afford the second one beside it.
 */
export interface ColonyWeek {
  readonly weekIndex: number;
  readonly state: ColonyWeekOutcomeState;
  readonly category: ColonyBossCategory | null;
  readonly materials: number;
  readonly efficiencyGain: number;
  readonly moraleDelta: number;
}

/**
 * The colony as it stands today.
 *
 * Mirrors `ColonyResponse` returned by `GET /api/colony`.
 *
 * One ceiling decides the score: `feedablePopulation`, what the food allows. `efficiency` is how far
 * one point of food carries, and it is the whole of what materials buy. Nothing is capped and nothing
 * is wasted, so the page has no arbitration to explain.
 */
export interface Colony {
  readonly runNumber: number;
  readonly runDay: number;
  readonly runDayCount: number;
  readonly runWeekIndex: number;
  readonly runWeekCount: number;

  /**
   * Calendar day this state closes, as an ISO-8601 date (`YYYY-MM-DD`).
   */
  readonly day: string;

  readonly population: number;

  /**
   * Inhabitants the night moved, negative when the town lost people.
   */
  readonly populationChange: number;

  /**
   * Inhabitants one point of food feeds, raised by the materials gathered. Never capped.
   *
   * The one figure joining the two halves of the page: the food rail counts food, the hexagon counts
   * inhabitants, and this is the rate between them. Drawn on the food rail as the factor it is,
   * `×12,44`, rather than left implicit — without it, every challenge and every fight paid into a
   * ladder of decorative names and the page never said what that bought.
   */
  readonly efficiency: number;

  readonly materials: number;

  /**
   * Materials this week's validated challenges have already secured, credited at the next rollover.
   *
   * Sent because `materials` only ever moves on a Monday, so it reads as a flat zero for the whole
   * first week of a run while the squad is in fact earning.
   */
  readonly pendingMaterials: number;

  /**
   * Food of the last seven days. A moving average, never a reserve.
   */
  readonly foodStock: number;

  /**
   * The seven daily harvests behind that stock, oldest first. Shorter than `foodWindowDays` while a
   * young run's window is still filling.
   */
  readonly foodWindow: readonly ColonyFoodDay[];

  /**
   * Days the window spans once full. What tells a window still filling — where nothing expires yet —
   * from one dropping its oldest day every night.
   */
  readonly foodWindowDays: number;

  readonly feedablePopulation: number;
  readonly weeklyConsumption: number;

  /**
   * Food left over once the town has eaten, never negative. What makes it grow.
   */
  readonly weeklySurplus: number;

  readonly presence: ColonyPresence;
  readonly morale: ColonyMorale;
  readonly tier: ColonyTier;
  readonly nextTier: ColonyTier;
  readonly tierProgressPercentage: number;
  readonly ladder: readonly ColonyTier[];
  readonly weeks: readonly ColonyWeek[];
  readonly defeatedBosses: number;
  readonly bossCount: number;
}

/**
 * One day of the population curve.
 *
 * Mirrors `ColonyTrajectoryPointResponse`. Carries the ceiling alongside the population, and the
 * efficiency that set it, so a step in the curve can be read as a Monday rather than as a good evening.
 */
export interface ColonyTrajectoryPoint {
  readonly day: string;
  readonly runDay: number;
  readonly population: number;
  readonly feedablePopulation: number;
  readonly efficiency: number;
  readonly materials: number;
  readonly foodStock: number;
  readonly morale: number;
  readonly presenceCount: number;
}

/**
 * The day the town crossed one step of its ladder, for the curve to be read against.
 *
 * Mirrors `ColonyMilestoneResponse`.
 */
export interface ColonyMilestone {
  readonly name: ColonyTierName;
  readonly level: number;
  readonly day: string;
  readonly runDay: number;
  readonly threshold: number;
}

/**
 * Population curve of the run in progress.
 *
 * Mirrors `ColonyTrajectoryResponse` returned by `GET /api/colony/trajectory`.
 */
export interface ColonyTrajectory {
  readonly runNumber: number;
  readonly runDayCount: number;
  readonly peakPopulation: number;
  readonly peakDay: string | null;

  /**
   * Mean population over the days played. What separates a run that was held from a hollow one that
   * ended at the same place.
   */
  readonly averagePopulation: number;
  readonly points: readonly ColonyTrajectoryPoint[];
  readonly milestones: readonly ColonyMilestone[];
}

/**
 * One closed run and how it ended.
 *
 * Mirrors `ColonyRunHistoryResponse` returned by `GET /api/colony/history`. There is deliberately no
 * share of a maximum: efficiency has no ceiling, so any percentage would be measured against a number
 * the model does not have.
 */
export interface ColonyRunHistory {
  readonly runNumber: number;
  readonly firstDay: string;

  /**
   * The run's final day: its settlement day for a run that ran its full course, or the day an
   * operator stopped it early.
   */
  readonly finalDay: string;

  /**
   * The run's score: the population of {@link finalDay}.
   */
  readonly finalPopulation: number;
  readonly peakPopulation: number;
  readonly averagePopulation: number;
  readonly efficiency: number;
  readonly materials: number;
  readonly tier: ColonyTier;
  readonly defeatedBosses: number;
  readonly bossCount: number;
}
