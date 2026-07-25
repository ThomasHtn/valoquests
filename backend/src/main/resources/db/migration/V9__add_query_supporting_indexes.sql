-- Supports the active-player lookup used by scheduled and manual synchronizations.
CREATE INDEX idx_player_status_id
  ON player(status, id);

-- Supports joins starting from a weekly challenge when loading player progress.
CREATE INDEX idx_player_challenge_progress_weekly_challenge_player
  ON player_challenge_progress(weekly_challenge_id, player_id);

-- Supports retrieval of the latest synchronization execution by business start time.
CREATE INDEX idx_synchronization_started_at_id
  ON synchronization(started_at DESC, id DESC);

-- Supports loading per-player results for one synchronization.
CREATE INDEX idx_synchronization_player_result_synchronization_player
  ON synchronization_player_result(synchronization_id, player_id);
