-- Players are initially inserted using their Riot game name and tag line.
-- The Riot PUUID is resolved during the first successful synchronization.
ALTER TABLE player
  ALTER COLUMN riot_puuid DROP NOT NULL;

-- Keep the raw Henrik queue identifier alongside the normalized game mode.
ALTER TABLE valorant_match
  ADD COLUMN queue_id VARCHAR(64);

-- Keep the player's team identifier as returned by the Henrik API.
ALTER TABLE player_match
  ADD COLUMN team_id VARCHAR(32);

-- Queue identifiers will be used for match filtering and challenge calculations.
CREATE INDEX idx_valorant_match_queue_id
  ON valorant_match(queue_id);
