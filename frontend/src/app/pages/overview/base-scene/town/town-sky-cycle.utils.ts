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
 * Viewer's local time of day in fractional hours, from epoch ms.
 */
export function hourOf(now: number): number {
  const date = new Date(now);
  return date.getHours() + date.getMinutes() / 60;
}

/**
 * Light and colours at a fractional hour (wrapped into [0, 24)), blending the two nearest keys.
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
 * Sun at an hour, `null` below the horizon.
 */
export function sunAt(hour: number): SkyBody | null {
  return bodyOnArc(hour, SUN_HOURS);
}

/**
 * Moon at an hour, `null` below the horizon.
 */
export function moonAt(hour: number): SkyBody | null {
  return bodyOnArc(hour < MOON_HOURS[0] ? hour + 24 : hour, MOON_HOURS);
}

/**
 * Blends two `#rrggbb` colours, `t` being the share of `b` in [0, 1].
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
 * Places a body on its arc, rising left and setting right.
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
 * Eases a ratio so each key holds a little before blending to the next.
 */
function smoothstep(t: number): number {
  const c = Math.min(1, Math.max(0, t));
  return c * c * (3 - 2 * c);
}
