import { AGENT_PORTRAIT_FILES } from './player-avatar.constants';

/**
 * Bundled avatar path of a player's `portrait`, `null` when absent or unknown.
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
