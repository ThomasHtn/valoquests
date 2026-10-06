-- Every open tab polls the synchronization status each minute; these keep its reads off a full scan
-- of a history that now grows by one execution every five minutes.

-- Supports the end of the last successful execution.
CREATE INDEX idx_synchronization_succeeded_finished_at
  ON synchronization(finished_at DESC, id DESC)
  WHERE status IN ('COMPLETED', 'PARTIAL');

-- Supports the end of the last successful execution that imported matches.
CREATE INDEX idx_synchronization_imported_finished_at
  ON synchronization(finished_at DESC)
  WHERE status IN ('COMPLETED', 'PARTIAL') AND matches_imported > 0;

-- Supports the "is one running" flag.
CREATE INDEX idx_synchronization_running
  ON synchronization(id)
  WHERE status = 'RUNNING';

-- Supports the check for matches imported since a player's last successful pass.
CREATE INDEX idx_player_match_player_created_at
  ON player_match(player_id, created_at);
