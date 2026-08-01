-- Records how confidently each match's game mode was resolved, so a later synchronization can enrich
-- it without ever downgrading or silently overwriting a manual correction.
--
-- Priority, highest first: MANUALLY_CORRECTED, PROVIDED, INFERRED, UNKNOWN. Every match imported
-- before this migration was resolved by the same tiered logic, just without recording which tier won;
-- PROVIDED is assumed for a resolved mode and UNKNOWN for GameMode.OTHER, which is the exact case the
-- default already covers.
ALTER TABLE valorant_match
    ADD COLUMN game_mode_source VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN';

UPDATE valorant_match
SET game_mode_source = 'PROVIDED'
WHERE game_mode <> 'OTHER';
