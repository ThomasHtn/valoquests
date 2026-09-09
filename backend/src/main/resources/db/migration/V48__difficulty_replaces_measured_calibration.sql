-- The campaign's difficulty replaces the calibration measured on match history.
--
-- The reference used to be averaged over a window of Henrik history, which counted every week a
-- player did not play as a zero. A squad whose window had never been backfilled therefore landed on
-- the floor and beat its week-one guardian by Wednesday. Nothing is measured any more: the operator
-- picks AMATEUR or PRO at opening, and the enum carries the reference every figure hangs off.
--
-- The existing squad_level held exactly this choice for the challenge grid alone, so it is renamed
-- rather than dropped and its two values are remapped: REFERENCE became AMATEUR, EXPERT became PRO.
-- Live campaigns keep the grid they were opened on; their guardians are resized by the next replay,
-- since AMATEUR carries 5 300 where the floor carried 3 500.

ALTER TABLE campaign
    RENAME COLUMN squad_level TO difficulty;

ALTER TABLE campaign
    DROP CONSTRAINT ck_campaign_squad_level;

UPDATE campaign
SET difficulty = CASE difficulty
                     WHEN 'EXPERT' THEN 'PRO'
                     ELSE 'AMATEUR'
                 END;

ALTER TABLE campaign
    ADD CONSTRAINT ck_campaign_difficulty CHECK (difficulty IN ('AMATEUR', 'PRO'));

-- Derived from the difficulty from now on, so storing them would only let a second copy drift.
ALTER TABLE campaign
    DROP CONSTRAINT ck_campaign_tier,
    DROP COLUMN reference,
    DROP COLUMN tier,
    DROP COLUMN calibration_window_months,
    DROP COLUMN calibration_first_day;
