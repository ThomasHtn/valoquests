-- Henrik reports a zero damage total for Skirmish, which was averaged into a 0 ADR. A zero total
-- now means "not reported", so the rows already stored drop their ADR to match.
UPDATE player_match
SET adr = NULL
WHERE damage_dealt = 0
  AND adr IS NOT NULL;
