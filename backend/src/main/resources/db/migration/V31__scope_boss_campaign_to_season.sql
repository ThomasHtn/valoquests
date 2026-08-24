-- Attaches every weekly boss fight to the Valorant act it was fought in, so the campaign restarts at
-- each act change instead of accumulating forever.
--
-- Recorded on the row rather than derived from the week's dates: seasons reach us from Henrik match
-- metadata with no start or end date (see V6), so there is no interval to place a week in. The act in
-- force when the fight was drawn is the only thing that can be known, and once written it is history
-- and never moves again.
--
-- Nullable on purpose: a fight can be drawn before any match of the roster has been imported, and a
-- campaign reset empties the season table without touching this one.
ALTER TABLE weekly_boss_encounter
    ADD COLUMN season_id BIGINT;

ALTER TABLE weekly_boss_encounter
    ADD CONSTRAINT fk_weekly_boss_encounter_season
        FOREIGN KEY (season_id) REFERENCES season (id);

CREATE INDEX idx_weekly_boss_encounter_season
    ON weekly_boss_encounter (season_id, week_start DESC);

-- Existing fights predate the column and would all drop out of the campaign the moment this deploys,
-- which is a reset nobody asked for. They are attached to the act currently in progress, so today's
-- campaign survives the migration and the next act change is what resets it.
--
-- Mirrors DefaultSeasonQueryService#chronologicalKey: acts order within an era, a year-era season
-- outranks every episode-era one, and a name matching neither spelling comes last.
UPDATE weekly_boss_encounter
SET season_id = (
    SELECT id
    FROM season
    ORDER BY
        CASE
            WHEN name ~* '^e[0-9]+a[0-9]+$'
                THEN substring(name from '^[eE]([0-9]+)')::BIGINT * 1000
                    + substring(name from '[aA]([0-9]+)$')::BIGINT
            WHEN name ~* '^v[0-9]{2}a[0-9]+$'
                THEN (2000 + substring(name from '^[vV]([0-9]+)')::BIGINT) * 1000
                    + substring(name from '[aA]([0-9]+)$')::BIGINT
            ELSE -1
        END DESC,
        id DESC
    LIMIT 1
)
WHERE season_id IS NULL;
