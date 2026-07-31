-- Records how far match-history synchronization walked each season, for each player.
--
-- complete = true means the season was walked back to its oldest match: the stored history for it is
-- contiguous, so a later run may safely stop at the first already-stored match.
--
-- complete = false means the walk was interrupted, by a rate limit, a crash or the safety page
-- limit, or has simply never finished. The next run then re-walks that season in full instead of
-- stopping early, which is what prevents an interruption from leaving a permanent hole.
--
-- The row is created when the walk of a season starts, so its mere presence also answers the
-- question asked at a season boundary: an older season with no row was never targeted and must be
-- left alone. That is what bounds a first run on an empty database to the current season, while
-- still letting a run finish a previous season it had started when Riot rolled the act over.
CREATE TABLE player_season_synchronization
(
    id           BIGSERIAL PRIMARY KEY,
    player_id    BIGINT      NOT NULL REFERENCES player (id),
    season_id    BIGINT      NOT NULL REFERENCES season (id),
    complete     BOOLEAN     NOT NULL DEFAULT false,
    completed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_player_season_synchronization UNIQUE (player_id, season_id)
);

-- No further index: uk_player_season_synchronization is a btree on (player_id, season_id) whose
-- leading column serves both lookups the repository performs.
