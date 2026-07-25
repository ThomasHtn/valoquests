-- Supports chronological challenge calculations after filtering by player.
CREATE INDEX idx_player_match_player_match
  ON player_match(player_id, match_id);

-- Supports match-history filtering and sorting through the joined match table.
CREATE INDEX idx_valorant_match_season_started_at
  ON valorant_match(season_id, started_at DESC, id DESC);

-- Supports active and finalized weekly-challenge reads.
CREATE INDEX idx_weekly_challenge_week_finalized_id
  ON weekly_challenge(week_start, finalized_at, id);

-- Supports ranking-history pagination over finalized weeks.
CREATE INDEX idx_weekly_player_score_finalized_week_position
  ON weekly_player_score(finalized_at, week_start DESC, position ASC);

-- Supports week-wide progress aggregation without table scans.
CREATE INDEX idx_player_challenge_progress_player_weekly
  ON player_challenge_progress(player_id, weekly_challenge_id);
