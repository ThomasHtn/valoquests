import { CompetitiveTierVisual } from '@core/players/competitive-tier.model';

/**
 * Identity of the adventurer the guided tour's closing step opens on, reduced to what that step
 * shows: a portrait, a name and a rank. The records beside it come from the profile's own
 * component, so nothing about them belongs here.
 */
export interface PlayerSpotlightIdentity {
  readonly displayName: string;
  readonly avatarUrl: string | null;
  readonly rankIconUrl: string | null;
  readonly tier: CompetitiveTierVisual;
}
