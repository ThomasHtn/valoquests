-- Re-categorizes matches imported before the SKIRMISH and NEW_MAP game modes existed.
--
-- Both queues were previously unknown to the Henrik mapper and landed in the OTHER bucket. The raw
-- Henrik queue slug kept in valorant_match.queue_id makes the reclassification deterministic.
--
-- Scoped to game_mode = 'OTHER' so a match already categorized is never rewritten, which keeps the
-- migration idempotent and prevents it from touching modes it does not own.

UPDATE valorant_match
SET game_mode = 'SKIRMISH'
WHERE game_mode = 'OTHER'
  AND queue_id IN ('skirmish_2v2', 'skirmish');

UPDATE valorant_match
SET game_mode = 'NEW_MAP'
WHERE game_mode = 'OTHER'
  AND queue_id = 'newmap';

-- One match kept a blank queue slug, which leaves nothing in the stored data to classify it: the
-- Skirmish maps it was played on belong to a ruleset, not to a queue, so they cannot tell a
-- Skirmish queue apart from a custom game using that ruleset. Henrik was queried for this match and
-- returned queue {"id": "", "name": "Custom Game", "mode_type": "Skirmish"}, which settles it as a
-- custom game. The correction is therefore addressed to that match alone rather than derived from a
-- rule the data does not support.
UPDATE valorant_match
SET game_mode = 'CUSTOM'
WHERE game_mode = 'OTHER'
  AND external_match_id = 'f8e47ddc-475d-4a8d-a847-6062b0563d42';
