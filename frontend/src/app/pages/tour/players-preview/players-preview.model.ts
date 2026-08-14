import { CompetitiveTierVisual } from '@core/players/competitive-tier.model';

/**
 * Single card of the guided tour's squad row: a tracked player reduced to what the tour shows —
 * a portrait, a rank and a name. Deliberately narrower than the players page's `PlayerRow`: the
 * tour states that profiles exist, it does not reproduce their statistics.
 */
export interface PlayerPreviewCard {
  readonly id: number;
  readonly displayName: string;
  readonly avatarUrl: string | null;
  readonly rankIconUrl: string | null;
  readonly tier: CompetitiveTierVisual;
}
