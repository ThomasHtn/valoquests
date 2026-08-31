import { ColonyTierGlyph } from './colony-view.model';
import { ColonyTierName } from './colony.model';

/**
 * The silhouette each name of the ladder wears.
 *
 * Five bands rather than one icon per name. Eighteen distinct glyphs in a panel this narrow would each
 * be read as a different kind of thing, when the point of the ladder is that it is one thing growing:
 * a camp, then houses, then a skyline, then a monument, then a sprawl with no edge left. Grouping also
 * keeps the icon set small enough that the panel stays legible at the size it is actually drawn at.
 *
 * The fifth band exists because the six steps past the citadel are exactly the ones the scene draws by
 * closing the horizon rather than by building on the quay: seven consecutive markers wearing the same
 * monument would have said the ladder had stopped, at the point where the campaign is at its best.
 *
 * The band carries the *step*. Whether that step is crossed, current or still locked is carried by the
 * marker's colour and fill, which is what freed the icon to say something else — it used to repeat the
 * state a solid green hexagon was already stating.
 */
const TIER_GLYPHS: Readonly<Record<ColonyTierName, ColonyTierGlyph>> = {
  CAMP: 'CAMP',
  HAMLET: 'CAMP',
  VILLAGE: 'HOUSES',
  BOROUGH: 'HOUSES',
  TOWN: 'HOUSES',
  CITY: 'SKYLINE',
  RESIDENTIAL_QUARTER: 'SKYLINE',
  GREAT_CITY: 'SKYLINE',
  METROPOLIS: 'SKYLINE',
  MEGALOPOLIS: 'MONUMENT',
  CAPITAL: 'MONUMENT',
  CITADEL: 'MONUMENT',
  CONURBATION: 'SPRAWL',
  MEGAREGION: 'SPRAWL',
  ARCOLOGY: 'SPRAWL',
  ECUMENOPOLIS: 'SPRAWL',
  CONTINUUM: 'SPRAWL',
  STRATUM: 'SPRAWL',
};

/**
 * Resolves the silhouette one step of the ladder is drawn with.
 *
 * @param tier - The step, or anything carrying its name.
 * @returns The glyph band it belongs to.
 */
export function tierGlyphFor(tier: { readonly name: ColonyTierName }): ColonyTierGlyph {
  return TIER_GLYPHS[tier.name];
}

/**
 * Reads one fight's efficiency gain as the share of a ladder step it covers.
 *
 * Taken in efficiency rather than in materials, because the ladder's steps *are* efficiency
 * thresholds: `tierProgressPercentage` — the position this share is drawn next to — is measured on
 * that same scale, and pricing the gain in materials instead would put two lengths on one rail that
 * were measured against two different totals.
 *
 * Deliberately not clamped. A boss worth more than the step still to climb really does happen, and
 * saying so is the point; a caller drawing a band clamps it to the rail it has, and states the
 * whole figure beside it.
 *
 * @param efficiencyGain - Efficiency the fight adds if it is won.
 * @param fromThreshold - Efficiency the colony's current step opened at.
 * @param toThreshold - Efficiency the next step opens at.
 * @returns The share of the step, in percent, or `null` when there is no step to measure against —
 *   two steps at the same threshold, or a fight worth no efficiency at all.
 */
export function tierShareOfGain(
  efficiencyGain: number,
  fromThreshold: number,
  toThreshold: number,
): number | null {
  const span = toThreshold - fromThreshold;

  return span <= 0 || efficiencyGain <= 0 ? null : (efficiencyGain / span) * 100;
}

/**
 * The twelve names, in the order the backend's own `TIER_NAMES` lists them.
 */
const TIER_ORDER: readonly ColonyTierName[] = [
  'CAMP',
  'HAMLET',
  'VILLAGE',
  'BOROUGH',
  'TOWN',
  'CITY',
  'RESIDENTIAL_QUARTER',
  'GREAT_CITY',
  'METROPOLIS',
  'MEGALOPOLIS',
  'CAPITAL',
  'CITADEL',
  'CONURBATION',
  'MEGAREGION',
  'ARCOLOGY',
  'ECUMENOPOLIS',
  'CONTINUUM',
  'STRATUM',
];

/**
 * Resolves the ladder step a named tier sits on, counted from zero.
 *
 * The step is the only open-ended reading of the ladder. The names cover the first eighteen steps and
 * `STRATUM`, the last of them, then repeats numbered by `level`, so a run that keeps climbing has a
 * name but no new *position* unless the number is folded back in: stratum 1 is step 17, stratum 2 is
 * step 18, and so on. The backend computes the same thing in reverse in `DefaultColonyRuleset.tierAt`.
 *
 * Anything drawing growth reads this rather than the name — a scene keyed on the name stops moving at
 * the twelfth step, which on a good run is the last third of the campaign spent looking at one
 * picture.
 *
 * @param tier - The step, or anything carrying its name and its number past the last name.
 * @returns Its position on the ladder, from zero.
 */
export function tierStepFor(tier: {
  readonly name: ColonyTierName;
  readonly level: number;
}): number {
  const named = TIER_ORDER.indexOf(tier.name);

  return tier.name === TIER_ORDER[TIER_ORDER.length - 1]
    ? TIER_ORDER.length - 2 + Math.max(1, tier.level)
    : Math.max(0, named);
}
