import {LEVELS, clampLevel, formatTime, grey, step} from '../src/drift';

describe('drift', () => {
  it('moves inside', () => expect(step(10, 5, 100)).toEqual([15, 5]));
  it('bounces off far edge', () => expect(step(98, 5, 100)).toEqual([97, -5]));
  it('bounces off near edge', () => expect(step(2, -5, 100)).toEqual([3, 5]));
  it('no room stays at zero', () => expect(step(10, 5, 0)[0]).toBe(0));

  it('level stays in range', () => {
    expect(clampLevel(-1)).toBe(0);
    expect(clampLevel(99)).toBe(LEVELS.length - 1);
    expect(clampLevel(2)).toBe(2);
  });

  it('grey and time format', () => {
    expect(grey(0)).toBe('#1e1e1e');
    expect(formatTime(new Date(2026, 0, 1, 7, 5))).toBe('07:05');
  });
});
