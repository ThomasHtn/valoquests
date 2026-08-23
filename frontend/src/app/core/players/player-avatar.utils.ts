/**
 * Agent portraits bundled under `public/player-avatars`, named after their exact Valorant agent
 * name. Exported so the backoffice can offer exactly the portraits the site can render.
 *
 * The backend's player `portrait` field is not populated yet (no synchronization step resolves it
 * from Henrik), so it is used here as the name of the player's associated agent (e.g. `"Sova"`)
 * rather than a ready-to-use image URL. This lets the ranking and player screens show a
 * recognizable icon today; it should be revisited once the backend serves real per-player
 * artwork.
 */
export const AGENT_PORTRAIT_FILES: readonly string[] = [
  'Killjoy',
  'Neon',
  'Omen',
  'Phoenix',
  'Skye',
  'Sova',
];

/**
 * Resolves the local avatar asset matching a player's `portrait` field.
 *
 * @param portrait - The player's `portrait` field, as returned by the API.
 * @returns The public asset path to the player's avatar, or `null` when the portrait is absent or
 * does not match a bundled agent, so callers can fall back to a placeholder icon.
 */
export function resolvePlayerAvatarUrl(portrait: string | null): string | null {
  if (!portrait) {
    return null;
  }

  const trimmed = portrait.trim();
  for (const agent of AGENT_PORTRAIT_FILES) {
    if (agent.toLowerCase() === trimmed.toLowerCase()) {
      return `/player-avatars/${agent}.webp`;
    }
  }

  return null;
}
