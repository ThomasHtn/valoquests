/**
 * The names a town wears as it grows, from the smallest to the largest.
 *
 * Mirrors the backend `ColonyTierName`. Purely decorative: crossing a step changes no rule, it only
 * gives the run a milestone roughly once a week. `CITADEL` is the open end of the ladder — past it
 * every further step is a numbered citadel, which is what lets the names run on without a maximum.
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
  | 'CITADEL';

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
  readonly state: ColonyTierState;
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

  /**
   * Lowest morale reachable, so a badly started run stays playable. Drawn on the bar rather than
   * implied, otherwise a value of 55 sitting at 44 % of the track makes its own figure a lie.
   */
  readonly floor: number;

  readonly ceiling: number;

  /**
   * Share of the gap tonight closes at this morale.
   */
  readonly growthPercentPerNight: number;
}

/**
 * What one week of the run's fights was worth to the colony.
 *
 * Mirrors `ColonyWeekResponse`. `housingGain` is what the map writes on the week's own territory:
 * materials are an intermediate currency the player never handles, and housing is the only part of a
 * fight's reward still standing on settlement day.
 */
export interface ColonyWeek {
  readonly weekIndex: number;
  readonly state: ColonyWeekOutcomeState;
  readonly category: ColonyBossCategory | null;
  readonly materials: number;
  readonly housingGain: number;
  readonly moraleDelta: number;
}

/**
 * The colony as it stands today.
 *
 * Mirrors `ColonyResponse` returned by `GET /api/colony`.
 *
 * Two ceilings decide the score and the lower one commands: `feedablePopulation` is what the food
 * allows, `capacity` is what the housing allows. They are handed over side by side, smallest first,
 * so the pair cannot be read the wrong way round.
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

  readonly capacity: number;
  readonly materials: number;

  /**
   * Food of the last seven days. A moving average, never a reserve.
   */
  readonly foodStock: number;

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
  readonly missingCapacity: number;
  readonly tierProgressPercentage: number;
  readonly ladder: readonly ColonyTier[];
  readonly weeks: readonly ColonyWeek[];
  readonly defeatedBosses: number;
  readonly bossCount: number;
}

/**
 * One day of the population curve.
 *
 * Mirrors `ColonyTrajectoryPointResponse`. Carries both ceilings alongside the population, because
 * the whole game reads off the three together: the town hugs whichever is lower.
 */
export interface ColonyTrajectoryPoint {
  readonly day: string;
  readonly runDay: number;
  readonly population: number;
  readonly feedablePopulation: number;
  readonly capacity: number;
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
 * share of a maximum: housing has no ceiling, so any percentage would be measured against a number
 * the model does not have.
 */
export interface ColonyRunHistory {
  readonly runNumber: number;
  readonly firstDay: string;
  readonly settlementDay: string;

  /**
   * The run's score: the population of its settlement day, once the tenth week's materials and boss
   * have been credited.
   */
  readonly finalPopulation: number;
  readonly peakPopulation: number;
  readonly averagePopulation: number;
  readonly capacity: number;
  readonly materials: number;
  readonly tier: ColonyTier;
  readonly defeatedBosses: number;
  readonly bossCount: number;
}
