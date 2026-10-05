import { describe, expect, it } from 'vitest';

import { campaign, translate, week } from './overview.fixtures';
import { buildFrieze, formatSigned } from './overview.utils';

describe('buildFrieze', () => {
  it('returns nothing outside a campaign', () => {
    expect(buildFrieze(null, translate)).toEqual([]);
  });

  it('names each week after its planet, its drawing and its guardian level, abbreviated', () => {
    const [entry] = buildFrieze(
      campaign({ weeks: [week({ weekIndex: 3, category: 'ELITE' })] }),
      translate,
    );

    expect(entry).toMatchObject({
      label: '03',
      name: 'Kepler',
      art: '/planets/planet-03.svg',
      level: 'overview.frieze.level.ELITE',
      title: 'overview.frieze.title(common.guardianCategory.ELITE,overview.frieze.ahead)',
    });
  });

  it('flags only the weeks Sunday has settled, whose report can be opened', () => {
    const [done, running] = buildFrieze(
      campaign({
        currentWeekIndex: 2,
        weeks: [week({ weekIndex: 1, settled: true }), week({ weekIndex: 2, defeated: true })],
      }),
      translate,
    );

    expect(done.settled).toBe(true);
    expect(running).toMatchObject({ state: 'won', settled: false });
  });

  it('marks a defeated week as won, its ring empty', () => {
    const [entry] = buildFrieze(campaign({ weeks: [week({ defeated: true })] }), translate);

    expect(entry).toMatchObject({
      state: 'won',
      standing: 0,
      status: 'overview.frieze.status.won',
    });
  });

  it('marks a settled but undefeated week as lost, quoting the breakthrough reached', () => {
    const [entry] = buildFrieze(
      campaign({ weeks: [week({ settled: true, progressPercent: 78 })] }),
      translate,
    );

    expect(entry).toMatchObject({
      state: 'lost',
      standing: 0.22,
      status: 'overview.frieze.status.lost(78)',
      title: 'overview.frieze.title(common.guardianCategory.STANDARD,overview.frieze.lost(78))',
    });
  });

  it('marks the week in progress as now, with its breakthrough', () => {
    const [entry] = buildFrieze(
      campaign({ currentWeekIndex: 1, weeks: [week({ progressPercent: 30 })] }),
      translate,
    );

    expect(entry).toMatchObject({
      state: 'now',
      standing: 0.7,
      status: 'overview.frieze.status.now(30)',
    });
  });

  it('marks a week still ahead as ahead', () => {
    const [entry] = buildFrieze(
      campaign({ currentWeekIndex: 1, weeks: [week({ weekIndex: 4 })] }),
      translate,
    );

    expect(entry).toMatchObject({ state: 'ahead', status: 'overview.frieze.status.ahead' });
  });

  it('announces the final unplayed week of a still-running campaign', () => {
    const [entry] = buildFrieze(
      campaign({
        currentWeekIndex: 1,
        weeks: [week({ weekIndex: 10 })],
      }),
      translate,
    );

    expect(entry).toMatchObject({ state: 'ahead', status: 'overview.frieze.status.final' });
  });

  it('calls an unplayed week of a closed campaign unplayed rather than the final', () => {
    const [entry] = buildFrieze(
      campaign({ status: 'CLOSED', currentWeekIndex: null, weeks: [week({ weekIndex: 10 })] }),
      translate,
    );

    expect(entry).toMatchObject({ state: 'ahead', status: 'overview.frieze.status.unplayed' });
    expect(entry.title).toBe(
      'overview.frieze.title(common.guardianCategory.STANDARD,overview.frieze.unplayed)',
    );
  });
});

describe('formatSigned', () => {
  it('signs gains with a plus, losses with a true minus, and leaves zero bare', () => {
    const format = (amount: number) => String(amount);
    expect(formatSigned(1200, format)).toBe('+1200');
    expect(formatSigned(-40, format)).toBe('−40');
    expect(formatSigned(0, format)).toBe('0');
  });
});
