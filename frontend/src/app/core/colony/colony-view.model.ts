import { ColonyBuilding, ColonyGauge } from './colony.model';

/**
 * One gauge, resolved into everything the page lays out.
 */
export interface ColonyGaugeView {
  readonly gauge: ColonyGauge;

  /**
   * Single letter carried by the gauge's hexagonal badge.
   */
  readonly initial: string;

  /**
   * Already-translated gauge name.
   */
  readonly label: string;

  readonly percentage: number;

  /**
   * Already-formatted value, to one decimal.
   */
  readonly valueLabel: string;

  /**
   * What the gauge is, in one already-translated sentence: what feeds it, how it is played and what
   * the day moved on it. The band draws the gauges as bars alone, so the hover card this fills is
   * the only place a reader is told what a bar means. The name and the figure are laid out around
   * it rather than baked in.
   */
  readonly descriptionLabel: string;

  /**
   * Tailwind background utility for the bar's fill and the gauge's hexagonal badge.
   */
  readonly fillClass: string;

  /**
   * Tailwind text utility matching {@link fillClass}.
   */
  readonly textClass: string;

  /**
   * Whether this gauge is the one currently setting the equilibrium population.
   */
  readonly isLimiting: boolean;

  /**
   * Level the bar draws its reference tick at: where this gauge settles if the recent rhythm
   * holds. What turns a bar sitting at 33 from a famine into a colony on its mark.
   */
  readonly equilibriumPercentage: number;

  /**
   * Already-formatted equilibrium, to one decimal, for the reading beside the bar.
   */
  readonly equilibriumLabel: string;
}

/**
 * How far a run has got with one building.
 */
export type ColonyBuildingState = 'erected' | 'next' | 'locked';

/**
 * One building tier, resolved into everything the page lays out.
 */
export interface ColonyBuildingView {
  readonly building: ColonyBuilding;

  /**
   * Already-translated building name.
   */
  readonly name: string;
  readonly state: ColonyBuildingState;

  /**
   * Already-formatted capacity the tier opens.
   */
  readonly capacityLabel: string;

  /**
   * Already-translated sub-line: what it cost and when it went up, or what is still missing.
   */
  readonly detailLabel: string;
}

/**
 * How one week of the run's boss row ended.
 */
export type ColonyBossState = 'defeated' | 'survived' | 'current' | 'upcoming';

/**
 * One of the run's ten fights, resolved into everything the page lays out.
 */
export interface ColonyBossView {
  /**
   * Week of the run, from one.
   */
  readonly weekIndex: number;
  readonly state: ColonyBossState;

  /**
   * Already-translated accessible name for the hexagon.
   */
  readonly label: string;

  /**
   * Already-formatted materials at stake on this week: what the fight brought in once it is won,
   * what it stands to bring in until then. The figure is the same either way — see
   * {@link materialsEarned} for which of the two it is.
   */
  readonly materialsLabel: string;

  /**
   * Whether those materials are banked or still on the table, which is what separates a haul from
   * a prize.
   */
  readonly materialsEarned: boolean;
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

  /**
   * Buildings earned over the three a run can put up, the free starting camp excluded.
   */
  readonly buildingsLabel: string;

  /**
   * Bosses put down over the ten a run holds.
   */
  readonly bossesLabel: string;
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
