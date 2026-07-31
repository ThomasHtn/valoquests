-- Removes the challenges filtered on a game mode synchronization no longer imports.
--
-- Swiftplay and Escalation matches are not stored any more, so a challenge counting them can never
-- progress: it would be drawn into a weekly pack, stay at zero for every player, and waste one of
-- the four difficulty slots of that week.
--
-- Deleted rather than disabled. A disabled row is a maintenance obligation for a mode the tracker
-- does not observe, and it would keep referencing a ChallengeGameMode constant that no longer
-- exists, breaking catalogue deserialization.
--
-- Expressed on the condition payload rather than on a list of challenge codes, so a challenge added
-- later with the same filter is caught by the same rule instead of surviving unnoticed.
--
-- Runs after V13 emptied weekly_challenge: that table has a foreign key to challenge, so the
-- catalogue rows can only be deleted once no weekly selection references them.
DELETE FROM challenge c
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements(c.conditions_json) AS condition
    WHERE condition ->> 'gameMode' IN ('SWIFTPLAY', 'ESCALATION')
);
