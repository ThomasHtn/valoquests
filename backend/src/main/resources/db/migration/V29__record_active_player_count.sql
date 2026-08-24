-- Records how many players each fight was sized for, so the next one can be calibrated from what the
-- roster actually produces rather than from a constant.
--
-- damage_dealt has been frozen at closure since V26, but on its own it says nothing: 60 000 from seven
-- players and 60 000 from three are different weeks. The divisor cannot be recomputed later either,
-- since the roster changes through the backoffice, so it is recorded alongside the fight it sized.
--
-- Defaults to 0, which is right for existing rows: they were sized before this was tracked, and
-- BossCalibrationService deliberately excludes rows with no recorded roster rather than guessing one.
ALTER TABLE weekly_boss_encounter
    ADD COLUMN active_player_count INTEGER NOT NULL DEFAULT 0;
