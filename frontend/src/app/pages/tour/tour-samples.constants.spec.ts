import { describe, expect, it } from 'vitest';

import {
  CAMPAIGN_WEEKS,
  CHALLENGE_WORTH,
  EXAMPLE_REFERENCE,
  GROUP_FACTOR,
  GUARDIAN_FACTOR,
  PROGRESSION_PER_WEEK,
} from '@pages/rules/rules.constants';
import {
  TOUR_SAMPLE_CAPACITY,
  TOUR_SAMPLE_CONTRIBUTION,
  TOUR_SAMPLE_DAILY,
  TOUR_SAMPLE_MISSION,
  TOUR_SAMPLE_STAKES,
} from './tour-samples.constants';

// The samples must follow the rules constants, so a changed constant fails here first.
describe('tour samples', () => {
  const week = CAMPAIGN_WEEKS[TOUR_SAMPLE_MISSION.weekIndex - 1];
  const crew = TOUR_SAMPLE_MISSION.crew;
  const progression = 1 + PROGRESSION_PER_WEEK * (TOUR_SAMPLE_MISSION.weekIndex - 1);

  it('sizes the boss and the group by the rules', () => {
    expect(TOUR_SAMPLE_MISSION.hitPoints).toBe(
      Math.round(EXAMPLE_REFERENCE * week.guardian * GUARDIAN_FACTOR * crew),
    );
    expect(TOUR_SAMPLE_MISSION.wounded).toBe(
      Math.round(EXAMPLE_REFERENCE * week.group * GROUP_FACTOR * crew * progression),
    );
  });

  it('adds the squad damage up to the hit points taken', () => {
    const damage = TOUR_SAMPLE_CONTRIBUTION.shares.reduce((sum, share) => sum + share.damage, 0);
    expect(damage).toBe(TOUR_SAMPLE_CONTRIBUTION.total);
    expect(TOUR_SAMPLE_MISSION.hitPoints - TOUR_SAMPLE_MISSION.hitPointsLeft).toBe(damage);
  });

  it('brings home one wounded per challenge point', () => {
    const points = TOUR_SAMPLE_CONTRIBUTION.shares.reduce(
      (sum, share) => sum + share.challengePoints,
      0,
    );
    expect(TOUR_SAMPLE_CAPACITY.fromChallenges).toBe(points);
  });

  it('extracts the smallest of group, seats and beds, times the breakthrough', () => {
    const capacity = TOUR_SAMPLE_CAPACITY;
    const reachable = Math.min(
      capacity.wounded - capacity.fromChallenges,
      capacity.carry.value,
      capacity.shelter.value,
    );
    const breach = TOUR_SAMPLE_CONTRIBUTION.total / TOUR_SAMPLE_MISSION.hitPoints;
    expect(capacity.fromGuardian).toBe(Math.floor(reachable * breach));
    expect(capacity.aboard).toBe(capacity.fromGuardian + capacity.fromChallenges);
    expect(capacity.leftBehind).toBe(capacity.wounded - capacity.aboard);
    expect(TOUR_SAMPLE_STAKES.gain).toBe(reachable - capacity.fromGuardian);
  });

  it('pays the daily challenge its weight in survivors', () => {
    const daily = CHALLENGE_WORTH.find((worth) => worth.difficulty === null);
    expect(TOUR_SAMPLE_DAILY.survivors).toBe(
      Math.round((EXAMPLE_REFERENCE * (daily?.weight ?? 0) * progression) / 1000),
    );
  });
});
