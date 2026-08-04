-- Weekly-boss gamification: the weekly ranking key becomes total damage (match damage + challenge
-- damage + regularity bonus + team bonus) instead of challenge points alone. `points` keeps its column
-- name and now holds challenge damage, resolved through the week's own scoring ruleset version rather
-- than challenge.points, which this feature supersedes for scoring purposes.
--
-- Existing rows predate matches/regularity/team bonuses contributing anything, so their total_damage is
-- backfilled to their existing points: that was, in effect, their whole total_damage under the previous
-- rules. Already-finalized weeks stay untouched otherwise (their stored position is not recalculated),
-- which preserves their historical ranking.
ALTER TABLE weekly_player_score
    ADD COLUMN match_damage INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN regularity_bonus INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN team_bonus INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN total_damage INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN active_days INTEGER NOT NULL DEFAULT 0;

UPDATE weekly_player_score
SET total_damage = points;
