import { TitleVisual } from '@core/campaign/titles/campaign-title-visual.model';
import { ExtractionLimiter } from '@core/campaign/campaign-week.model';
import { WeeklyTitle } from '@core/campaign/titles/campaign-title.model';

/**
 * Weekly title in the report, with its holder or nobody.
 */
export interface MissionReportTitle extends TitleVisual {
  /**
   * Which weekly title.
   */
  readonly key: WeeklyTitle;

  /**
   * Holder's name, `null` when nobody earned it.
   */
  readonly holder: string | null;

  /**
   * Bundled agent portrait name, or `null` when none was chosen.
   */
  readonly portrait: string | null;
}

/**
 * Fatal blow, split so the player's name can stand out.
 */
export interface MissionReportBlow {
  /**
   * Sentence opening (`Guardian defeated on Sunday at 15:04`).
   */
  readonly when: string;

  /**
   * Who dealt the blow, `null` when unknown.
   */
  readonly by: string | null;

  /**
   * Map, mode and score with leading punctuation, empty when unknown.
   */
  readonly where: string;
}

/**
 * Week's champion in the report, or nobody.
 */
export interface MissionReportChampion {
  /**
   * Name of the champion, or `null` on a shared first place.
   */
  readonly holder: string | null;

  /**
   * Bundled agent portrait name, or `null` when none was chosen.
   */
  readonly portrait: string | null;

  /**
   * Champion's ranking points, `0` when nobody won outright.
   */
  readonly points: number;
}

/**
 * A settled week as the Monday report tells it.
 */
export interface MissionReport {
  /**
   * Monday of the week, ISO date.
   */
  readonly weekStart: string;

  /**
   * One-based index of the week in the campaign.
   */
  readonly weekIndex: number;

  /**
   * Name of the planet.
   */
  readonly planetName: string;

  /**
   * Whether the guardian was defeated.
   */
  readonly defeated: boolean;

  /**
   * Share of the guardian’s hit points taken, in percent.
   */
  readonly breachPercent: number;

  /**
   * Fatal blow, `null` when the guardian held.
   */
  readonly blow: MissionReportBlow | null;

  /**
   * Inhabitants lost to the guardian left standing.
   */
  readonly baseLoss: number;

  /**
   * Wounded brought home.
   */
  readonly rescued: number;

  /**
   * Wounded spotted on the planet.
   */
  readonly spotted: number;

  /**
   * Share of the spotted wounded brought home, from 0 to 1.
   */
  readonly rescuedShare: number;

  /**
   * Wounded rescued through validated challenges.
   */
  readonly byChallenges: number;

  /**
   * Wounded extracted by the ship.
   */
  readonly byShip: number;

  /**
   * Wounded left on the planet.
   */
  readonly leftBehind: number;

  /**
   * What capped the extraction: a stock, the group, or nothing.
   */
  readonly limiter: ExtractionLimiter;

  /**
   * Week's harvest and base at its close, `null` when no day was replayed.
   */
  readonly base: {
    /**
     * Food gained over the week.
     */
    readonly foodGained: number;

    /**
     * Components gained over the week.
     */
    readonly componentsGained: number;

    /**
     * Inhabitants at the week's close.
     */
    readonly population: number;

    /**
     * Change of population over the week.
     */
    readonly populationChange: number;
  } | null;

  /**
   * Four titles, `null` when the week's ranking was never frozen.
   */
  readonly titles: readonly MissionReportTitle[] | null;

  /**
   * Champion, `null` when the week's ranking was never frozen.
   */
  readonly champion: MissionReportChampion | null;

  /**
   * Planet ahead, `null` after the last week.
   */
  readonly next: {
    /**
     * Name of the planet.
     */
    readonly planetName: string;

    /**
     * Hit points the guardian started with.
     */
    readonly hitPoints: number;

    /**
     * Wounded waiting on the planet.
     */
    readonly wounded: number;
  } | null;
}
