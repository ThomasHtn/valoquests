import { CompetitiveTier } from '@core/players/competitive-tier.model';
import { KeyFigureIcon, KeyFigureTone } from '../key-figures/key-figures.model';

/**
 * A move on the ladder between two seasons, ready to display.
 */
export interface RankDelta {
  /**
   * The move, e.g. `+2 divisions` or `Stable`.
   */
  readonly label: string;

  /**
   * Arrow drawn beside it.
   */
  readonly icon: KeyFigureIcon;

  /**
   * Whether the move is a gain, a loss or neither.
   */
  readonly tone: KeyFigureTone;
}

/**
 * One rank named with its badge, as the tooltip shows it.
 */
export interface RankLabel {
  /**
   * Translated rank name.
   */
  readonly label: string;

  /**
   * URL of the rank badge, or `null` when it cannot be resolved.
   */
  readonly iconUrl: string | null;
}

/**
 * What one point of the chart stands for: a season of a multi-season selection, or a match of a
 * single-season one.
 */
export type RankJourneyMode = 'seasons' | 'matches';

/**
 * The plotted line, whichever unit its points stand for.
 */
export interface RankJourneySeries {
  /**
   * What one point stands for.
   */
  readonly mode: RankJourneyMode;

  /**
   * Rank at each point, oldest first.
   */
  readonly tiers: readonly CompetitiveTier[];

  /**
   * Axis label of each point.
   */
  readonly labels: readonly (string | string[])[];

  /**
   * Index of the point holding the peak: the first to reach the highest rank.
   */
  readonly peakIndex: number;

  /**
   * The highest rank held.
   */
  readonly peakTier: CompetitiveTier;

  /**
   * Whether the peak point's own rank is the peak, so the enlarged badge replaces its marker.
   */
  readonly peakOnPoint: boolean;

  /**
   * Lowest ladder position the chart must show.
   */
  readonly lowestOrdinal: number;

  /**
   * Highest ladder position the chart must show.
   */
  readonly highestOrdinal: number;
}

/**
 * Content of the tooltip shown over one point of the rank journey.
 */
export interface RankJourneyTooltip {
  /**
   * What the point is: a season name, or a match's position in the season.
   */
  readonly heading: string;

  /**
   * Rank at that point.
   */
  readonly tier: RankLabel;

  /**
   * Tailwind text colour of that rank.
   */
  readonly tierClass: string;

  /**
   * What the rank is: the season's final one, or the one held after the match.
   */
  readonly caption: string;

  /**
   * Move since the previous point, or `null` for the first one.
   */
  readonly delta: RankDelta | null;

  /**
   * Caption of that move.
   */
  readonly deltaCaption: string;

  /**
   * Lowest rank held during a season, or `null` when it never left one rank or the point is a match.
   */
  readonly lowest: RankLabel | null;

  /**
   * Highest rank held during a season, or `null` when it never left one rank or the point is a match.
   */
  readonly highest: RankLabel | null;

  /**
   * Competitive matches of the season, or `null` for a match.
   */
  readonly matchesPlayed: number | null;

  /**
   * Wins and win rate of the season, formatted, or `null` for a match.
   */
  readonly wins: string | null;
}

/**
 * Sizes, in pixels, of the badges drawn on the chart at one breakpoint.
 */
export interface RankJourneyIconSizes {
  /**
   * Badge marking where a season ended.
   */
  readonly point: number;

  /**
   * Enlarged badge marking the peak.
   */
  readonly peak: number;

  /**
   * Width of the rail spanning a season's lowest and highest ranks.
   */
  readonly rail: number;
}
