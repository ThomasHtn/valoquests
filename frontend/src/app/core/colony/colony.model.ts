/**
 * The structures a colony erects, cheapest first.
 *
 * Mirrors the backend `ColonyBuilding` enum.
 */
export type ColonyBuilding = 'CAMP' | 'BARRACKS' | 'RESIDENTIAL_QUARTER' | 'CITADEL';

/**
 * The two gauges a colony's health is the geometric mean of.
 *
 * Mirrors the backend `ColonyGauge` enum. Food is fed by the day's match damage — playing every
 * day — and Energy by the day's turnout — playing together.
 */
export type ColonyGauge = 'FOOD' | 'ENERGY';

/**
 * One gauge and the day's movement on it.
 *
 * Mirrors `ColonyGaugeResponse`.
 */
export interface ColonyGaugeState {
  /**
   * Today's value, from 0 to 100.
   */
  readonly value: number;

  /**
   * What the day brought in, before the ceiling clamped it.
   */
  readonly gain: number;

  /**
   * What the day cost, `14 × (population / capacity)`, identical on both gauges.
   */
  readonly loss: number;
}

/**
 * One building tier and whether the run has reached it.
 *
 * Mirrors `ColonyBuildingResponse`.
 */
export interface ColonyBuildingTier {
  readonly building: ColonyBuilding;
  readonly materialsThreshold: number;
  readonly capacity: number;
  readonly erected: boolean;

  /**
   * Day of the run it went up on, or `null` while it has not.
   */
  readonly erectedOnRunDay: number | null;
}

/**
 * The tier the run is working towards.
 *
 * Mirrors `ColonyNextTierResponse`.
 */
export interface ColonyNextTier {
  readonly building: ColonyBuilding;
  readonly materialsThreshold: number;
  readonly capacity: number;
  readonly missingMaterials: number;
  readonly progressPercentage: number;
}

/**
 * The colony as it stands today.
 *
 * Mirrors `ColonyResponse` returned by `GET /api/colony`.
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
  readonly food: ColonyGaugeState;
  readonly energy: ColonyGaugeState;
  readonly healthPercentage: number;

  /**
   * Whether health has fallen under the distress threshold. A display flag with no mechanical
   * effect: a gauge at zero collapses the population, but a single day of play lifts it again and
   * there is no game over.
   */
  readonly alert: boolean;
  readonly population: number;
  readonly targetPopulation: number;
  readonly populationChange: number;
  readonly dailyMigrationLimit: number;
  readonly capacity: number;
  readonly maximumCapacity: number;
  readonly materials: number;
  readonly buildings: readonly ColonyBuildingTier[];

  /**
   * `null` once the last tier is up.
   */
  readonly nextTier: ColonyNextTier | null;

  /**
   * Gauge currently setting the equilibrium population: the one fed the least.
   */
  readonly limitingGauge: ColonyGauge;

  /**
   * Share of capacity the colony plateaus at while today's inputs hold,
   * `min(Food gain, Energy gain) / 14`. The model's fixed point, and the only thing about it that
   * cannot be read off the gauges themselves.
   */
  readonly equilibriumPercentage: number;
  readonly defeatedBosses: number;
  readonly bossCount: number;

  /**
   * Materials one defeated boss brings in. Resolved by the backend rather than restated here: the
   * frontend renders what the API returns, it never carries a calibration of its own.
   */
  readonly materialsPerBoss: number;
}

/**
 * One day of the population curve.
 *
 * Mirrors `ColonyTrajectoryPointResponse`.
 */
export interface ColonyTrajectoryPoint {
  readonly day: string;
  readonly runDay: number;
  readonly population: number;
  readonly capacity: number;
  readonly materials: number;
  readonly food: number;
  readonly energy: number;
  readonly activePlayerCount: number;
}

/**
 * The day a building went up, for the curve to be read against.
 *
 * Mirrors `ColonyMilestoneResponse`.
 */
export interface ColonyMilestone {
  readonly building: ColonyBuilding;
  readonly day: string;
  readonly runDay: number;
  readonly capacity: number;
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
   * Mean population over the days played. What separates a run that was held from a hollow one
   * that ended at the same place.
   */
  readonly averagePopulation: number;
  readonly points: readonly ColonyTrajectoryPoint[];
  readonly milestones: readonly ColonyMilestone[];
}

/**
 * One closed run and how it ended.
 *
 * Mirrors `ColonyRunHistoryResponse` returned by `GET /api/colony/history`.
 */
export interface ColonyRunHistory {
  readonly runNumber: number;
  readonly firstDay: string;
  readonly settlementDay: string;

  /**
   * The run's score: the population of its settlement day, once the tenth week's materials and
   * boss have been credited.
   */
  readonly finalPopulation: number;
  readonly maximumPercentage: number;
  readonly peakPopulation: number;
  readonly averagePopulation: number;
  readonly erectedBuildings: number;
  readonly buildingCount: number;
  readonly defeatedBosses: number;
  readonly bossCount: number;
}
