import { ColonyTierGlyph } from './colony-view.model';
import { ColonyTierName } from './colony.model';

/**
 * The silhouette each name of the ladder wears.
 *
 * Four bands rather than one icon per name. Twelve distinct glyphs in a panel this narrow would each
 * be read as a different kind of thing, when the point of the ladder is that it is one thing growing:
 * a camp, then houses, then a skyline, then a monument. Grouping also keeps the icon set small enough
 * that the panel stays legible at the size it is actually drawn at.
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
