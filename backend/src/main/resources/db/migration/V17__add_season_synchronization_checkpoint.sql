-- Adds a page-level checkpoint to the per-player, per-season synchronization state.
--
-- Until now, only the season-wide `complete` flag was persisted: an interruption forced the next run
-- to re-walk an unfinished season from offset zero. That is safe, but expensive for a heavy player,
-- whose season may require dozens of rate-limited Henrik requests to traverse. A transient failure
-- late in such a walk could keep costing the same early pages again on every retry, making the walk
-- less likely to ever reach the boundary that marks the season complete.
--
-- `next_start_offset` records the pagination offset proven, by a page that was itself durably
-- imported, to still belong to the season being walked. A resumed walk starts from there instead of
-- zero, skipping only the range a previous execution already confirmed and committed.
--
-- Default zero preserves current behavior for existing rows: an incomplete season already in progress
-- simply resumes from the start, exactly as before this migration.
ALTER TABLE player_season_synchronization
    ADD COLUMN next_start_offset INTEGER NOT NULL DEFAULT 0;
