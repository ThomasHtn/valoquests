-- Replaces the Valorant act with a ten-week "run" as the campaign's boundary, and lays the colony on
-- top of it.
--
-- An act has no regular duration, so two campaigns fought in two acts were never comparable. A run is
-- exactly ten weekly rollovers, which makes every run comparable to every other one by construction and
-- gives the campaign map a fixed size of ten hexagons.

-- One run: ten consecutive weeks, opened and closed by the weekly rollover.
--
-- `roster_size` is frozen here on purpose. It is the denominator of the colony's Energy gauge, and the
-- backoffice can activate, deactivate or archive a player at any time; reading it live would let an
-- archive rewrite the history of a run that has already been played.
--
-- `last_week_start` is derivable from `first_week_start` and the ruleset's run length, but is stored so
-- a run's span is readable from the row itself and so a future change to that length cannot retroactively
-- move the boundary of a run already under way.
CREATE TABLE run (
    id BIGSERIAL PRIMARY KEY,
    number INTEGER NOT NULL,
    first_week_start DATE NOT NULL,
    last_week_start DATE NOT NULL,
    roster_size INTEGER NOT NULL,
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_run_number UNIQUE (number),
    CONSTRAINT uk_run_first_week_start UNIQUE (first_week_start)
);

-- One row per day and per run: the whole colony, day by day.
--
-- The single table the colony needs. Its current state is the latest snapshot and its erected buildings
-- are a pure function of its materials, so there is nothing else to persist. The engine never mutates a
-- state incrementally: it replays the run in progress from its first day and rewrites these rows, which
-- is what makes a late synchronization, a three-day outage and an admin-triggered recompute all produce
-- the same numbers.
--
-- Gauges and population are NUMERIC rather than integers because the arithmetic is fractional
-- (14 x population / capacity, +2.5% of capacity per day) and a replay has to be reproducible to the
-- digit. Rounding happens at the API boundary, never in storage.
--
-- `food_gain`, `energy_gain` and `active_player_count` are not needed to rebuild the state — the replay
-- recomputes them — but they are what a recalibration would have to look at, and keeping them here means
-- doing it without asking Henrik for anything again.
CREATE TABLE colony_daily_snapshot (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES run (id),
    day DATE NOT NULL,
    food NUMERIC(9, 3) NOT NULL,
    energy NUMERIC(9, 3) NOT NULL,
    materials INTEGER NOT NULL,
    population NUMERIC(11, 3) NOT NULL,
    capacity INTEGER NOT NULL,
    active_player_count INTEGER NOT NULL,
    food_gain NUMERIC(9, 3) NOT NULL,
    energy_gain NUMERIC(9, 3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_colony_daily_snapshot_run_day UNIQUE (run_id, day)
);

CREATE INDEX idx_colony_daily_snapshot_run_day
    ON colony_daily_snapshot (run_id, day);

-- The campaign moves from the act to the run.
--
-- Existing encounters are deleted rather than backfilled. There is no production data to preserve, and
-- there is no honest run to attach a past fight to: runs are ten-week windows that did not exist when
-- those fights were drawn, so any mapping would be invented. Run 1 opens on a clean base at the first
-- rollover following this deployment.
--
-- `season_id` is dropped rather than left in place, for the reason V27 spelled out when it removed
-- `ruleset_version`: a column nothing reads any more is a column that drifts.
DELETE FROM weekly_boss_encounter;

DROP INDEX IF EXISTS idx_weekly_boss_encounter_season;

ALTER TABLE weekly_boss_encounter
    DROP CONSTRAINT IF EXISTS fk_weekly_boss_encounter_season;

ALTER TABLE weekly_boss_encounter
    DROP COLUMN season_id;

ALTER TABLE weekly_boss_encounter
    ADD COLUMN run_id BIGINT NOT NULL REFERENCES run (id);

CREATE INDEX idx_weekly_boss_encounter_run
    ON weekly_boss_encounter (run_id, week_start DESC);
