import { ColonyGauge } from './colony.model';

/**
 * The two Tailwind utilities a gauge is drawn in: the bar's fill and the figure beside it.
 */
export interface ColonyGaugeColors {
  readonly fill: string;
  readonly text: string;
}

/**
 * Tailwind utilities each gauge is drawn in, in its ordinary state.
 *
 * Food green and Energy blue: both come from the application's existing accent set, and neither is
 * the brand amber the population itself is drawn in, so a gauge is never mistaken for the figure it
 * feeds.
 */
const GAUGE_COLORS: Record<ColonyGauge, ColonyGaugeColors> = {
  FOOD: {
    fill: 'bg-accent-green',
    text: 'text-accent-green',
  },
  ENERGY: {
    fill: 'bg-accent-blue',
    text: 'text-accent-blue',
  },
};

/**
 * Tailwind utilities both gauges fall back to while the colony is in distress.
 *
 * The desaturated `danger` red rather than `accent-red`, which marks boss damage everywhere else:
 * a colony in trouble is a state, not a hit taken.
 */
const DISTRESS_COLORS: ColonyGaugeColors = {
  fill: 'bg-danger',
  text: 'text-danger',
};

/**
 * Resolves the utilities one gauge is drawn in.
 *
 * Shared by the campaign page's resource band and the overview's summary of it, so a gauge is the
 * same color on both screens — the summary exists precisely to be recognised again on the page it
 * links to.
 *
 * @param gauge - Which gauge this is.
 * @param alert - Whether the colony has fallen under the distress threshold.
 * @returns The fill and text utilities.
 */
export function colonyGaugeColors(gauge: ColonyGauge, alert: boolean): ColonyGaugeColors {
  return alert ? DISTRESS_COLORS : GAUGE_COLORS[gauge];
}

/**
 * Resolves the utilities health is drawn in.
 *
 * Health is never fed and has no accent of its own: it is the neutral bar the two colored ones
 * produce, and only turns red with the rest of the band.
 *
 * @param alert - Whether the colony has fallen under the distress threshold.
 * @returns The fill and text utilities.
 */
export function colonyHealthColors(alert: boolean): ColonyGaugeColors {
  return alert ? DISTRESS_COLORS : { fill: 'bg-text-primary/50', text: 'text-text-primary' };
}
