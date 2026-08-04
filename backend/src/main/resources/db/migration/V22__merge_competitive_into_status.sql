-- The `competitive` flag is merged into `status`: an INACTIVE player is now the single mechanism
-- for excluding a player from ranking slots and boss damage, while still being synchronized and
-- still completing challenges individually. The pro showcase player (id=7) moves to INACTIVE.
UPDATE player SET status = 'INACTIVE' WHERE id = 7;

ALTER TABLE player DROP COLUMN competitive;
