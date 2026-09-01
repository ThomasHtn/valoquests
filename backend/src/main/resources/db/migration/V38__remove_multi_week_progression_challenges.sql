-- Removes the challenges measuring a player against their own four previous weeks.
--
-- A weekly pack exists to be won inside its own week: the boss it feeds is drawn on Monday and
-- resolved on Sunday. A BASELINE challenge breaks that contract twice. It asks a question whose
-- answer was already half-decided before the week opened, and it compares two *rates* rather than
-- two efforts, so a player absent for three of the four baseline weeks holds a reference built on a
-- handful of matches: noisy, and easier to beat the less they played. The mode rewarded exactly the
-- absence the weekly loop exists to discourage.
--
-- Expressed on `progress_mode` rather than on a list of challenge codes, for the reason V14 gives:
-- a challenge added later with the same mode is caught by the same rule instead of surviving
-- unnoticed.
--
-- Deleted rather than disabled, unlike V28. That choice has a price and it is paid here: the rows
-- are referenced by `weekly_challenge`, so the selections that drew them and the progress recorded
-- against them go too. Two consequences, both intended:
--
--   * A past week that drew one of these keeps four challenges instead of five. Its frozen
--     `weekly_player_score` is untouched — the ranking history still reads as it was lived — but
--     the colony replays materials from `player_challenge_progress`, so the run's population is
--     recomputed slightly lower. Run `POST /api/admin/colony/recompute` after this migration.
--   * The week in progress loses one difficulty from its pack. It repairs itself: the recalculation
--     that follows every synchronization calls `selectWeekChallenges`, which draws only the missing
--     tiers and keeps the four already in play. `POST /api/admin/challenges/progress/recalculation`
--     forces it immediately instead of waiting for the next sync.
--
-- `ProgressMode.BASELINE` and its calculator stay in the code: nothing else declares the mode any
-- more, but keeping them costs one dormant enum constant and leaves the door open to a progression
-- challenge that is scoped to a single week.

DELETE FROM player_challenge_progress
WHERE weekly_challenge_id IN (
    SELECT weekly_challenge.id
    FROM weekly_challenge
    JOIN challenge ON challenge.id = weekly_challenge.challenge_id
    WHERE challenge.progress_mode = 'BASELINE'
);

DELETE FROM weekly_challenge
WHERE challenge_id IN (
    SELECT id FROM challenge WHERE progress_mode = 'BASELINE'
);

DELETE FROM challenge WHERE progress_mode = 'BASELINE';
