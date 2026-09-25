/**
 * Parses a numeric identifier read from a route parameter.
 *
 * @param raw - The raw route parameter.
 * @returns The identifier, or `null` when the parameter is not a positive integer.
 */
export function parseRouteId(raw: string): number | null {
  if (!/^\d+$/.test(raw)) {
    return null;
  }
  const id = Number(raw);
  return Number.isSafeInteger(id) && id > 0 ? id : null;
}
