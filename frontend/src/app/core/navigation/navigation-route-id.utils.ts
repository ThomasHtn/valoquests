/**
 * Route parameter as an id, `null` unless a positive safe integer.
 */
export function parseRouteId(raw: string): number | null {
  if (!/^\d+$/.test(raw)) {
    return null;
  }
  const id = Number(raw);
  return Number.isSafeInteger(id) && id > 0 ? id : null;
}
