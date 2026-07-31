-- Wipes every piece of data derived from Henrik match history, so synchronization restarts from a
-- coherent base under the new season scope and game-mode filter.
--
-- The stored history predates both rules: it holds matches of modes no longer imported, and seasons
-- that were never walked to their oldest match. Nothing records how far each season was actually
-- retrieved, so the existing rows cannot be repaired in place, only rebuilt.
--
-- Deliberately kept: the player rows seeded by V5, and the challenge catalogue. Everything else
-- listed here is recomputed, either by the next synchronization or by the challenge and ranking
-- recalculation that follows it.
--
-- Weekly challenge selections and their progression go too. They are derived from matches that no
-- longer exist, and a finalized week whose matches were deleted would report results nothing in the
-- database can justify. Rebuilding from an empty state is the only option that keeps rankings
-- traceable to stored matches.
--
-- TRUNCATE is atomic and resets the sequences of data that is entirely rebuilt. CASCADE is
-- deliberately omitted and every referencing table listed instead: Postgres then accepts the
-- statement only while the list stays complete, so a table added later cannot be silently emptied
-- by this migration.
TRUNCATE TABLE
    player_challenge_progress,
    weekly_player_score,
    weekly_challenge,
    synchronization_player_result,
    synchronization,
    player_match,
    valorant_match,
    season
RESTART IDENTITY;

-- Clears the incremental synchronization watermark of every player. Left as it is, it would claim a
-- history that no longer exists.
UPDATE player
SET last_successful_synchronization_at = NULL,
    updated_at = now();
