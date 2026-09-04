-- Gameplay v2, challenges: a cadence on the catalogue, a resolved copy of the rule on each draw.
--
-- The catalogue gains a daily pool next to the weekly tiers. A daily challenge has no difficulty:
-- it is its own tier, priced by its cadence, so the column becomes nullable and a check keeps the
-- two in step.
--
-- Every number in conditions_json is now a base target, written for the squad the catalogue was
-- calibrated on. The draw resolves it against the campaign in force and stores the result on the
-- selection row in resolved_conditions_json; calculators only ever read that copy. Stored rather
-- than recomputed on read because a campaign is replayed from its first day after every
-- synchronization, and a target that moved with the roster would rewrite the objectives of weeks
-- already played.
--
-- Selection history goes. V41 rewrites the catalogue entirely (codes, filters, targets, shapes),
-- so the rows drawn from the old one reference challenges that no longer exist and progress that
-- no longer means anything. The weekly scores were computed from that progress with the v1 barème
-- and go with it: the v2 ranking rewrites their columns.

TRUNCATE player_challenge_progress, weekly_challenge, weekly_player_score;

ALTER TABLE challenge
    ADD COLUMN cadence VARCHAR(10) NOT NULL DEFAULT 'WEEKLY',
    ALTER COLUMN difficulty DROP NOT NULL,
    ADD CONSTRAINT ck_challenge_daily_has_no_difficulty
        CHECK ((cadence = 'DAILY') = (difficulty IS NULL));

ALTER TABLE weekly_challenge
    ADD COLUMN cadence VARCHAR(10) NOT NULL DEFAULT 'WEEKLY',
    ADD COLUMN day DATE,
    ADD COLUMN resolved_conditions_json JSONB NOT NULL,
    ADD CONSTRAINT ck_weekly_challenge_daily_has_day
        CHECK ((cadence = 'DAILY') = (day IS NOT NULL));

-- One weekly row per challenge and week, one daily row per day. Two partial indexes rather than
-- one constraint: a shrunken daily pool may have to repeat a challenge inside one week, which the
-- old (week_start, challenge_id) constraint would have refused.
ALTER TABLE weekly_challenge DROP CONSTRAINT uk_weekly_challenge_week_challenge;

CREATE UNIQUE INDEX uk_weekly_challenge_week_challenge
    ON weekly_challenge (week_start, challenge_id)
    WHERE cadence = 'WEEKLY';

CREATE UNIQUE INDEX uk_weekly_challenge_daily_day
    ON weekly_challenge (day)
    WHERE cadence = 'DAILY';
