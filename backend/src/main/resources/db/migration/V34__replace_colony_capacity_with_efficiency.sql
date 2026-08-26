-- The colony grew towards the lower of two ceilings: what its food could feed, and what its housing
-- could lodge. Housing never bound on the day the score was read -- the seven-day food window means
-- settlement day always sees a full week of production -- so the materials that bought it were worth
-- 0.2% of a run. Materials now raise how far one point of food carries instead, which is worth 28%.
--
-- No backfill: the replay rewrites every snapshot of a run from its first day, so the first tick after
-- this migration produces correct values on its own.

ALTER TABLE colony_daily_snapshot
    DROP COLUMN capacity;

ALTER TABLE colony_daily_snapshot
    ADD COLUMN efficiency NUMERIC(6, 3) NOT NULL DEFAULT 8;

ALTER TABLE colony_daily_snapshot
    ALTER COLUMN efficiency DROP DEFAULT;
