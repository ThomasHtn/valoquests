-- The app no longer awards "points": challenges and matches deal damage. Renamed to match the
-- domain vocabulary already used everywhere else (match_damage, total_damage, ScoringRuleset).
ALTER TABLE challenge RENAME COLUMN points TO damage;
ALTER TABLE weekly_player_score RENAME COLUMN points TO challenge_damage;
