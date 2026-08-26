-- Rewrites the colony's only table for the food model.
--
-- The two 0-100 gauges, the daily loss and the geometric-mean health they produced are gone. What the
-- town has to eat is now a seven-day moving window of harvests, what it can lodge is a continuous
-- function of the frozen roster and the materials, and how fast it closes the gap between the two is
-- morale, which only the weekly boss moves.
--
-- Dropped and recreated rather than altered. Every column here is derived from rows that are still
-- there — imported matches, completed challenges, boss outcomes, the run's frozen roster — and
-- ColonyReplayService never mutates a snapshot incrementally: it replays the run in progress from its
-- first day and rewrites the lot. Migrating the old columns would mean inventing a food stock out of a
-- gauge that never measured one.

DROP TABLE IF EXISTS colony_daily_snapshot;

CREATE TABLE colony_daily_snapshot
(
    id                BIGSERIAL PRIMARY KEY,
    run_id            BIGINT         NOT NULL REFERENCES run (id),
    day               DATE           NOT NULL,

    -- Food of the last seven days: the whole of what the town has to eat. Never a reserve, always a
    -- moving average, so it can neither be banked nor collapse from one quiet evening.
    food_stock        NUMERIC(11, 3) NOT NULL,

    -- What this day alone brought in, turnout multiplier included.
    food_harvest      NUMERIC(11, 3) NOT NULL,

    -- Match damage of the day, after the scoring ruleset's daily diminishing returns. Not needed to
    -- rebuild the state, kept so the calibration can be revisited later without asking Henrik again.
    match_damage      INTEGER        NOT NULL,

    -- Players whose raw damage that day cleared the turnout threshold.
    presence_count    INTEGER        NOT NULL,

    -- Morale the day ends on, between the ruleset's floor and its ceiling.
    morale            NUMERIC(6, 2)  NOT NULL,

    -- Cumulative materials, which never go back down.
    materials         INTEGER        NOT NULL,

    -- Housing the frozen roster and the materials open.
    capacity          INTEGER        NOT NULL,

    population        NUMERIC(11, 3) NOT NULL,

    -- What the night moved, negative when the town lost people.
    population_change NUMERIC(11, 3) NOT NULL,

    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT uk_colony_daily_snapshot_run_day UNIQUE (run_id, day)
);

CREATE INDEX idx_colony_daily_snapshot_run_day ON colony_daily_snapshot (run_id, day);
