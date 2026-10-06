-- Active days always equalled streak days: the streak counts the played days of the week, gaps
-- included, and restarts every Monday. Rows written by an older streak formula are the only ones
-- where the two differ, and nothing reads the column any more.
ALTER TABLE weekly_player_score DROP COLUMN active_days;
