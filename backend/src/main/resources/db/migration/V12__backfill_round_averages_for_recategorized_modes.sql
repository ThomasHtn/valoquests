-- Rebuilds the per-round averages of matches re-categorized by V11.
--
-- Skirmish and New Map matches were imported while their queue was still unrecognized, so they were
-- treated as non round-based and their acs/adr were stored as NULL. Both modes do play scored
-- rounds, which makes the averages recoverable from the raw totals already persisted.
--
-- Mirrors HenrikMatchMapper.average: total / rounds_played, two decimals, rounded half up. Postgres
-- rounds numeric values half away from zero, which is equivalent here as totals are never negative.
--
-- ADR is only rebuilt when damage was actually reported. Henrik omits the damage breakdown for
-- Skirmish, which the import stores as a zero total because the column is not nullable. Deriving an
-- average from it would replace an honestly absent value by a zero that pollutes player averages.
--
-- Deliberately left untouched: deathmatch and team deathmatch, whose NULL averages are correct.
-- Their round count is unrelated to scoring, so a computed average would be meaningless.
UPDATE player_match pm
SET acs = round(pm.score::numeric / pm.rounds_played, 2),
    adr = CASE
              WHEN pm.damage_dealt > 0
                  THEN round(pm.damage_dealt::numeric / pm.rounds_played, 2)
              ELSE NULL
          END,
    updated_at = now()
FROM valorant_match m
WHERE m.id = pm.match_id
  -- CUSTOM is included for the single match V11 moved out of the OTHER bucket, whose averages were
  -- never computed either. The acs IS NULL guard leaves every other custom match untouched.
  AND m.game_mode IN ('SKIRMISH', 'NEW_MAP', 'CUSTOM')
  -- Restricting to unset averages keeps the migration idempotent and prevents it from overwriting
  -- a value computed at import time.
  AND pm.acs IS NULL
  AND pm.rounds_played > 0;

-- Clears the ADR of every match imported before the mapper stopped deriving an average from an
-- absent damage total. Those rows store adr = 0.00 while no damage was ever reported, which counts
-- as a real zero in player averages instead of being excluded from them. Custom games are the only
-- ones affected today, but the condition is expressed on the data rather than on the mode so it
-- also covers any other queue for which Henrik omitted the damage breakdown.
UPDATE player_match
SET adr = NULL,
    updated_at = now()
WHERE damage_dealt = 0
  AND adr IS NOT NULL;
