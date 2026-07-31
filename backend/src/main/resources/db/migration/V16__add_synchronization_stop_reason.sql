-- Records why the match-history walk stopped, for each player of a synchronization execution.
--
-- The walk already computes this fact to decide whether a season may be marked complete; storing it
-- makes an incomplete import self-explanatory. SEASON_BOUNDARY answers "why does this player have
-- fewer matches than his tracker profile shows" without any manual database or Henrik inspection,
-- and PAGE_LIMIT_REACHED distinguishes a truncated run from a healthy one.
--
-- Nullable on purpose: a player whose synchronization failed never completed a walk, so it has no
-- stop reason to report. Rows written before this column keep NULL, their stop reason being
-- unrecoverable.
ALTER TABLE synchronization_player_result
    ADD COLUMN stop_reason VARCHAR(30);
