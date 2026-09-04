-- The weekly ranking becomes v2: guardian damage plus challenge points.
--
-- Challenge damage, the regularity bonus and the team bonus are gone: a challenge no longer hurts
-- anything, regularity is already paid by the streak inside every match, and there is no team bonus.
-- What a week produced in food and components is stored next to the damage, so the four weekly
-- titles can be read back off a finalized week between two campaigns, when no campaign day exists.
--
-- Rows are derived data rebuilt by every recalculation, and the ranking key changes meaning, so the
-- table is emptied rather than converted: a v1 total read as v2 points would rank a week nobody
-- played under these rules.
TRUNCATE weekly_player_score;

ALTER TABLE weekly_player_score
    DROP COLUMN challenge_damage,
    DROP COLUMN match_damage,
    DROP COLUMN regularity_bonus,
    DROP COLUMN team_bonus,
    DROP COLUMN total_damage,
    ADD COLUMN guardian_damage            INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN food                       INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN components                 INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN match_count                INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN streak_days                INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN challenge_points           INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN completed_daily_challenges INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN total_points               INTEGER NOT NULL DEFAULT 0;
