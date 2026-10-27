// Pure logic of Dormir, ported from DormirActivity.java (tv-otimizada) so it can be tested without a device.

// Grey levels for the clock, darkest first. 0x38 (22%) is the default; the TV decides how low it can go.
export const LEVELS = [0x1e, 0x2a, 0x38, 0x48, 0x5c, 0x74];
export const DEFAULT_LEVEL = 2;

export function clampLevel(level: number): number {
  return Math.max(0, Math.min(LEVELS.length - 1, level));
}

export function grey(level: number): string {
  const h = LEVELS[clampLevel(level)].toString(16).padStart(2, '0');
  return `#${h}${h}${h}`;
}

/** Next position on one axis: moves by v and bounces inside [0, max]. Returns [position, velocity]. */
export function step(pos: number, v: number, max: number): [number, number] {
  if (max <= 0) return [0, v];
  let p = pos + v;
  if (p < 0) { p = -p; v = -v; }
  if (p > max) { p = 2 * max - p; v = -v; }
  return [Math.max(0, Math.min(max, p)), v];
}

export function formatTime(d: Date): string {
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}
