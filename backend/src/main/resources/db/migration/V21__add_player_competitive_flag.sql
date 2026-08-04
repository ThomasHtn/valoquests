-- Non-competitive players (e.g. Natank) still get their account tracked and synchronized like anyone
-- else, but never take part in weekly challenge resolution, boss combat or ranking positions. This is
-- independent from `status`, which continues to govern synchronization eligibility.
ALTER TABLE player ADD COLUMN competitive BOOLEAN NOT NULL DEFAULT true;

UPDATE player SET competitive = false WHERE id = 7;

-- A non-competitive player still gets a weekly score for display purposes, just never a ranking slot.
ALTER TABLE weekly_player_score ALTER COLUMN position DROP NOT NULL;
