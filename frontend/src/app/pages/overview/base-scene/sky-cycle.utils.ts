import { SkyBody, SkyState } from './town-scene.model';
import {
  ARC_TOP,
  HORIZON,
  MOON_HOURS,
  SKY_KEYS,
  SUN_HOURS,
  TOWN_WIDTH,
} from './town-scene.constants';

/**
 * The light of the scene, read from the viewer's clock.
 *
 * A handful of keyed hours (deep night, dawn, morning, noon, late afternoon, dusk, blue hour) and
 * a smooth blend between the two surrounding the current time: the base drifts through the day
 * instead of switching between four fixed pictures.
 */

/**
 * Local time of day, in fractional hours.
 *
 * @param now - Epoch milliseconds.
 */
export function hourOf(now: number): number {
  const date = new Date(now);
  return date.getHours() + date.getMinutes() / 60;
}

/**
 * Light and colours of the scene at an hour.
 *
 * @param hour - Fractional hour; wrapped into [0, 24).
 */
export function skyAt(hour: number): SkyState {
  const h = ((hour % 24) + 24) % 24;
  const next = SKY_KEYS.findIndex((key) => key.hour > h);
  const from = SKY_KEYS[next - 1];
  const to = SKY_KEYS[next];
  const t = smoothstep((h - from.hour) / (to.hour - from.hour));

  const mixed: Record<string, string | number> = {};
  for (const field of Object.keys(from.sky) as (keyof SkyState)[]) {
    const a = from.sky[field];
    const b = to.sky[field];
    mixed[field] =
      typeof a === 'number' ? a + ((b as number) - a) * t : mixColor(a, b as string, t);
  }
  return mixed as unknown as SkyState;
}

/**
 * The sun at an hour, or null while it is below the horizon.
 */
export function sunAt(hour: number): SkyBody | null {
  return bodyOnArc(hour, SUN_HOURS);
}

/**
 * The moon at an hour, or null while it is below the horizon.
 */
export function moonAt(hour: number): SkyBody | null {
  return bodyOnArc(hour < MOON_HOURS[0] ? hour + 24 : hour, MOON_HOURS);
}

/**
 * Blends two `#rrggbb` colours.
 *
 * @param t - Share of `b`, in [0, 1].
 */
export function mixColor(a: string, b: string, t: number): string {
  const channel = (hex: string, i: number): number => parseInt(hex.slice(1 + i * 2, 3 + i * 2), 16);
  let out = '#';
  for (let i = 0; i < 3; i++) {
    const value = Math.round(channel(a, i) + (channel(b, i) - channel(a, i)) * t);
    out += value.toString(16).padStart(2, '0');
  }
  return out;
}

/**
 * Places a body on its arc, left horizon at its rise, right horizon at its set.
 */
function bodyOnArc(hour: number, [rise, set]: readonly [number, number]): SkyBody | null {
  const progress = (hour - rise) / (set - rise);
  if (progress < 0 || progress > 1) {
    return null;
  }
  const elevation = Math.sin(Math.PI * progress);
  return {
    x: 70 + progress * (TOWN_WIDTH - 140),
    y: HORIZON - 4 - elevation * (HORIZON - 4 - ARC_TOP),
    elevation,
  };
}

/**
 * Eases a linear ratio so a key is held a little before the blend to the next one starts.
 */
function smoothstep(t: number): number {
  const c = Math.min(1, Math.max(0, t));
  return c * c * (3 - 2 * c);
}
