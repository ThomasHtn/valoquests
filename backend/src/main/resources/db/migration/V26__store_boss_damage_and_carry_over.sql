-- Freezes each encounter's damage at closure, and records the hit points it inherited from a surviving
-- predecessor.
--
-- V19 deliberately did not store damage, deriving it from weekly_player_score so there would be a single
-- source of truth. That reasoning no longer holds now that a surviving boss passes its remainder to the
-- next fight: an admin recalculating a finalized week through /api/admin would move that week's derived
-- damage, and therefore the hit points of a following week that had already been fought and closed. The
-- carry-over chain has to be immutable, so the number it reads from is now persisted.
--
-- Both columns default to 0, which is exactly right for existing rows: encounters closed before this
-- migration inherited nothing, and their damage stays available through the weekly scores they were
-- resolved from.
ALTER TABLE weekly_boss_encounter
    ADD COLUMN carried_over_hp INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN damage_dealt INTEGER NOT NULL DEFAULT 0;
