/**
 * The three tracks the resource band draws, in the order it draws them.
 *
 * Not three gauges of the same kind: food is a quantity read against two ceilings, turnout is a head
 * count read against the frozen roster, morale is a speed read against a hundred. They share a
 * grammar — glyph socket, rail, figure, subtitle — and nothing else.
 */
export type ColonyTrack = 'FOOD' | 'PRESENCE' | 'MORALE';

/**
 * The Tailwind utilities one track is drawn in.
 */
export interface ColonyTrackColors {
  /**
   * The band carrying the track's own value.
   */
  readonly fill: string;

  /**
   * The same hue, muted: what the town already eats on the food rail, the unreachable floor on the
   * morale one. Both are the track's own quantity, so both are its own colour.
   */
  readonly muted: string;

  /**
   * The figure set beside the rail, and the glyph in the socket.
   */
  readonly text: string;
}

/**
 * Tailwind utilities each track is drawn in.
 *
 * Food keeps the brand amber the population is drawn in, because it is the same quantity seen twice:
 * the bar says what the town eats and what it has left to grow on, and the hexagon beside it says how
 * far that has got. Turnout and morale take accents of their own so a rail is never mistaken for the
 * figure it feeds.
 */
const TRACK_COLORS: Record<ColonyTrack, ColonyTrackColors> = {
  FOOD: {
    fill: 'bg-brand-500',
    muted: 'bg-brand-500/30',
    text: 'text-brand-500',
  },
  PRESENCE: {
    fill: 'bg-accent-cyan',
    muted: 'bg-accent-cyan/30',
    text: 'text-accent-cyan',
  },
  MORALE: {
    fill: 'bg-accent-violet',
    muted: 'bg-accent-violet/35',
    text: 'text-accent-violet',
  },
};

/**
 * Resolves the utilities one track is drawn in.
 *
 * Shared by the campaign page's resource band and the overview's summary of it, so a track is the
 * same colour on both screens — the summary exists precisely to be recognised again on the page it
 * links to.
 *
 * @param track - Which track this is.
 * @returns The fill, muted and text utilities.
 */
export function colonyTrackColors(track: ColonyTrack): ColonyTrackColors {
  return TRACK_COLORS[track];
}
