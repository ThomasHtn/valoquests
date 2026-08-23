/**
 * Riot's episode-era season code, such as `e10a3` (episode 10, act 3).
 */
const EPISODE_SEASON_PATTERN = /^e(\d{1,2})a(\d{1,2})$/i;

/**
 * Riot's year-era season code, such as `V26A4` (2026, act 4) — the naming that replaced episodes.
 */
const YEAR_SEASON_PATTERN = /^v(\d{2})a(\d{1,2})$/i;

/**
 * Spells a raw season code out into the labels Riot's own clients use.
 *
 * Seasons reach us as the short code Henrik reports (`e10a3`, `V26A4`), which is stored verbatim
 * and means nothing to a reader. Both eras are handled: an unrecognized code is returned as-is
 * rather than mangled, since Riot has already renamed its seasons once.
 *
 * @param name - The raw season name, as returned by the API.
 * @param translate - Dictionary lookup, taken as a parameter so this stays a pure function.
 * @returns The spelled-out label, or `name` itself when the code matches no known era.
 */
export function formatSeasonName(
  name: string,
  translate: (key: string, params?: Readonly<Record<string, string | number>>) => string,
): string {
  const episode = EPISODE_SEASON_PATTERN.exec(name);
  if (episode) {
    return translate('seasons.episode', { episode: Number(episode[1]), act: Number(episode[2]) });
  }

  const year = YEAR_SEASON_PATTERN.exec(name);
  if (year) {
    return translate('seasons.year', { year: 2000 + Number(year[1]), act: Number(year[2]) });
  }

  return name;
}
