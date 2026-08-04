-- ============================================================================================
-- DEV-ONLY FIXTURE SCRIPT — NOT A FLYWAY MIGRATION.
--
-- This file lives outside `src/main/resources/db/migration` on purpose, so Flyway (which only
-- scans that classpath location) can never pick it up. Never move it there and never let it be
-- committed as a migration: it plants fake historical data, which must never ship to a real
-- environment.
--
-- Purpose: populate one finalized past week with plausible matches, weekly scores, challenge
-- progress and a boss encounter, so the boss page and the ranking history page can be visually
-- reviewed against real-looking data without waiting for the app to actually play out a week.
--
-- Week chosen: Monday 2026-06-08 to Sunday 2026-06-14. NOT the calendar week immediately before
-- today (2026-08-04) — this local dev database already holds real synchronized matches and real
-- weekly_player_score/weekly_challenge rows from 2026-06-24 onward, including a fully real week
-- 2026-07-27. 2026-06-08 predates all of that and was verified empty in every one of the tables
-- below before writing this script, so it cannot collide with or corrupt genuine data.
--
-- Usage: run manually against your local dev database once it is up and migrated, e.g.:
--   docker compose up -d
--   ./mvnw spring-boot:run   # applies migrations up to V21 first, then stop it
--   psql "$DB_URL" -U "$DB_USERNAME" -f backend/scripts/dev-seed/seed-past-week.sql
--
-- Requires migration V21 (player.competitive) to already be applied, so player 7 (Natank) is
-- flagged non-competitive and the nullable weekly_player_score.position column exists.
--
-- Idempotency: NOT idempotent. Re-running this script fails on the unique constraints
-- (external_match_id, week_start/challenge_id, player_id/week_start, week_start on the boss
-- encounter). Reset with the `DROP SCHEMA public CASCADE` recipe from backend/CLAUDE.md before
-- re-running, or adjust the week_start/external_match_id literals below to a different, still-empty
-- week.
-- ============================================================================================

-- ------------------------------------------------------------------------------------------------
-- 1. Matches: a handful of competitive (and one deathmatch) game spread across the week for the six
--    competitive players, plus five standalone matches for Natank (player 7) showcasing pro-level
--    play. Every match uses the most recent season row (this dev DB's only season row is
--    `active = false`, so filtering on `active = true` would resolve to NULL and violate the
--    `season_id NOT NULL` constraint).
-- ------------------------------------------------------------------------------------------------
INSERT INTO valorant_match (
  external_match_id, season_id, started_at, duration_seconds, map_id, map_name,
  game_mode, game_mode_source, queue_id, red_score, blue_score
)
VALUES
  ('seed-0608-m1', (SELECT id FROM season ORDER BY id DESC LIMIT 1), '2026-06-08T19:00:00Z', 2400, 'ascent', 'Ascent', 'COMPETITIVE', 'PROVIDED', 'competitive', 7, 13),
  ('seed-0609-m1', (SELECT id FROM season ORDER BY id DESC LIMIT 1), '2026-06-09T20:00:00Z', 2600, 'bind', 'Bind', 'COMPETITIVE', 'PROVIDED', 'competitive', 7, 13),
  ('seed-0609-m2', (SELECT id FROM season ORDER BY id DESC LIMIT 1), '2026-06-09T21:15:00Z', 2200, 'haven', 'Haven', 'COMPETITIVE', 'PROVIDED', 'competitive', 9, 13),
  ('seed-0610-m1', (SELECT id FROM season ORDER BY id DESC LIMIT 1), '2026-06-10T18:30:00Z', 2100, 'icebox', 'Icebox', 'COMPETITIVE', 'PROVIDED', 'competitive', 6, 13),
  ('seed-0610-m2', (SELECT id FROM season ORDER BY id DESC LIMIT 1), '2026-06-10T20:00:00Z', 2500, 'split', 'Split', 'COMPETITIVE', 'PROVIDED', 'competitive', 13, 10),
  ('seed-0611-m1', (SELECT id FROM season ORDER BY id DESC LIMIT 1), '2026-06-11T19:45:00Z', 2700, 'lotus', 'Lotus', 'COMPETITIVE', 'PROVIDED', 'competitive', 11, 13),
  ('seed-0612-m1', (SELECT id FROM season ORDER BY id DESC LIMIT 1), '2026-06-12T21:00:00Z', 2300, 'sunset', 'Sunset', 'COMPETITIVE', 'PROVIDED', 'competitive', 9, 13),
  ('seed-0613-m1', (SELECT id FROM season ORDER BY id DESC LIMIT 1), '2026-06-13T19:00:00Z', 2400, 'ascent', 'Ascent', 'COMPETITIVE', 'PROVIDED', 'competitive', 8, 13),
  ('seed-0613-m2', (SELECT id FROM season ORDER BY id DESC LIMIT 1), '2026-06-13T22:00:00Z', 900, 'district', 'District', 'DEATHMATCH', 'PROVIDED', 'deathmatch', NULL, NULL),
  ('seed-0614-m1', (SELECT id FROM season ORDER BY id DESC LIMIT 1), '2026-06-14T20:00:00Z', 2500, 'pearl', 'Pearl', 'COMPETITIVE', 'PROVIDED', 'competitive', 9, 13),
  ('seed-nat-0608', (SELECT id FROM season ORDER BY id DESC LIMIT 1), '2026-06-08T21:00:00Z', 2000, 'haven', 'Haven', 'COMPETITIVE', 'PROVIDED', 'competitive', 4, 13),
  ('seed-nat-0609', (SELECT id FROM season ORDER BY id DESC LIMIT 1), '2026-06-09T19:00:00Z', 2000, 'fracture', 'Fracture', 'COMPETITIVE', 'PROVIDED', 'competitive', 5, 13),
  ('seed-nat-0611', (SELECT id FROM season ORDER BY id DESC LIMIT 1), '2026-06-11T20:00:00Z', 1900, 'ascent', 'Ascent', 'COMPETITIVE', 'PROVIDED', 'competitive', 3, 13),
  ('seed-nat-0613', (SELECT id FROM season ORDER BY id DESC LIMIT 1), '2026-06-13T18:00:00Z', 2000, 'bind', 'Bind', 'COMPETITIVE', 'PROVIDED', 'competitive', 5, 13),
  ('seed-nat-0614', (SELECT id FROM season ORDER BY id DESC LIMIT 1), '2026-06-14T19:00:00Z', 2100, 'split', 'Split', 'COMPETITIVE', 'PROVIDED', 'competitive', 6, 13);

-- ------------------------------------------------------------------------------------------------
-- 2. Player-match rows. team_id is consistent with each match's red_score/blue_score above, so
--    result (WIN/LOSS) always matches the team that actually won.
-- ------------------------------------------------------------------------------------------------
INSERT INTO player_match (
  player_id, match_id, team_id, agent_id, agent_name, result, kills, deaths, assists, score,
  headshots, bodyshots, legshots, damage_dealt, rounds_played, acs, adr, competitive_tier,
  rank_rating, was_mvp
)
VALUES
  ((SELECT id FROM player WHERE display_name = 'Psilonnix'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0608-m1'), 'Blue', 'jett', 'Jett', 'WIN', 22, 14, 5, 5400, 8, 10, 4, 3200, 20, 270.0, 160.0, 'DIAMOND_2', 62, false),
  ((SELECT id FROM player WHERE display_name = 'kikoucraft'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0608-m1'), 'Blue', 'omen', 'Omen', 'WIN', 15, 12, 9, 4600, 5, 12, 3, 2500, 20, 230.0, 125.0, 'PLATINUM_3', 44, false),
  ((SELECT id FROM player WHERE display_name = 'Psilonnix'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0609-m1'), 'Red', 'jett', 'Jett', 'LOSS', 16, 17, 4, 4100, 6, 11, 5, 2600, 20, 205.0, 130.0, 'DIAMOND_2', 60, false),
  ((SELECT id FROM player WHERE display_name = 'NoWayToLearn'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0609-m1'), 'Red', 'sova', 'Sova', 'LOSS', 12, 16, 8, 3400, 3, 10, 4, 1900, 20, 170.0, 95.0, 'GOLD_1', 30, false),
  ((SELECT id FROM player WHERE display_name = 'kikoucraft'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0609-m2'), 'Blue', 'omen', 'Omen', 'WIN', 19, 13, 6, 4900, 7, 9, 2, 2900, 22, 223.0, 132.0, 'PLATINUM_3', 45, false),
  ((SELECT id FROM player WHERE display_name = 'getjfox'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0609-m2'), 'Blue', 'killjoy', 'Killjoy', 'WIN', 14, 11, 7, 3900, 4, 8, 3, 2100, 22, 177.0, 105.0, 'GOLD_2', 34, false),
  ((SELECT id FROM player WHERE display_name = 'Psilonnix'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0610-m1'), 'Blue', 'jett', 'Jett', 'WIN', 25, 10, 3, 6100, 11, 9, 2, 3600, 19, 321.0, 190.0, 'DIAMOND_2', 63, true),
  ((SELECT id FROM player WHERE display_name = 'NoWayToLearn'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0610-m2'), 'Red', 'sova', 'Sova', 'WIN', 18, 12, 10, 4700, 6, 10, 3, 2700, 23, 204.0, 118.0, 'GOLD_1', 31, false),
  ((SELECT id FROM player WHERE display_name = 'DuffManBzH'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0610-m2'), 'Red', 'sage', 'Sage', 'WIN', 10, 14, 12, 3200, 2, 7, 2, 1600, 23, 139.0, 78.0, 'SILVER_3', 20, false),
  ((SELECT id FROM player WHERE display_name = 'Psilonnix'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0611-m1'), 'Blue', 'jett', 'Jett', 'WIN', 21, 15, 6, 5200, 9, 10, 3, 3000, 24, 216.0, 140.0, 'DIAMOND_2', 64, false),
  ((SELECT id FROM player WHERE display_name = 'kikoucraft'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0611-m1'), 'Blue', 'omen', 'Omen', 'WIN', 13, 16, 8, 3800, 4, 9, 4, 2000, 24, 158.0, 100.0, 'PLATINUM_3', 46, false),
  ((SELECT id FROM player WHERE display_name = 'Izakiel'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0611-m1'), 'Blue', 'reyna', 'Reyna', 'WIN', 17, 14, 5, 4300, 6, 11, 3, 2400, 24, 179.0, 112.0, 'GOLD_3', 38, false),
  ((SELECT id FROM player WHERE display_name = 'getjfox'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0612-m1'), 'Red', 'killjoy', 'Killjoy', 'LOSS', 11, 18, 4, 2900, 3, 8, 5, 1700, 22, 132.0, 88.0, 'GOLD_2', 33, false),
  ((SELECT id FROM player WHERE display_name = 'Psilonnix'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0613-m1'), 'Blue', 'jett', 'Jett', 'WIN', 24, 11, 5, 5800, 10, 9, 3, 3400, 21, 276.0, 175.0, 'DIAMOND_2', 65, true),
  ((SELECT id FROM player WHERE display_name = 'NoWayToLearn'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0613-m1'), 'Blue', 'sova', 'Sova', 'WIN', 14, 13, 9, 3900, 4, 9, 4, 2200, 21, 186.0, 110.0, 'GOLD_1', 32, false),
  ((SELECT id FROM player WHERE display_name = 'Izakiel'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0613-m2'), NULL, 'reyna', 'Reyna', 'WIN', 20, 15, 0, 4000, 8, 10, 2, 2800, 1, 0.0, 0.0, 'GOLD_3', 39, false),
  ((SELECT id FROM player WHERE display_name = 'kikoucraft'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0614-m1'), 'Red', 'omen', 'Omen', 'LOSS', 12, 17, 7, 3300, 3, 9, 5, 1800, 22, 150.0, 92.0, 'PLATINUM_3', 44, false),
  ((SELECT id FROM player WHERE display_name = 'DuffManBzH'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-0614-m1'), 'Red', 'sage', 'Sage', 'LOSS', 8, 15, 11, 2700, 1, 6, 3, 1300, 22, 122.0, 70.0, 'SILVER_3', 19, false),
  ((SELECT id FROM player WHERE display_name = 'MDR nataNk'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-nat-0608'), 'Blue', 'jett', 'Jett', 'WIN', 31, 6, 4, 7800, 18, 7, 1, 4600, 18, 433.0, 260.0, 'RADIANT', 720, true),
  ((SELECT id FROM player WHERE display_name = 'MDR nataNk'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-nat-0609'), 'Blue', 'raze', 'Raze', 'WIN', 29, 8, 6, 7300, 16, 8, 2, 4300, 18, 405.0, 245.0, 'RADIANT', 722, true),
  ((SELECT id FROM player WHERE display_name = 'MDR nataNk'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-nat-0611'), 'Blue', 'jett', 'Jett', 'WIN', 33, 5, 3, 8200, 19, 6, 1, 4900, 16, 512.0, 305.0, 'RADIANT', 725, true),
  ((SELECT id FROM player WHERE display_name = 'MDR nataNk'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-nat-0613'), 'Blue', 'chamber', 'Chamber', 'WIN', 27, 9, 7, 6900, 14, 9, 2, 4000, 18, 383.0, 230.0, 'RADIANT', 726, false),
  ((SELECT id FROM player WHERE display_name = 'MDR nataNk'), (SELECT id FROM valorant_match WHERE external_match_id = 'seed-nat-0614'), 'Blue', 'jett', 'Jett', 'WIN', 30, 7, 5, 7500, 17, 8, 1, 4500, 19, 395.0, 237.0, 'RADIANT', 728, true);

-- ------------------------------------------------------------------------------------------------
-- 3. Weekly challenges: one per difficulty, mirroring how the real weekly selection works.
-- ------------------------------------------------------------------------------------------------
INSERT INTO weekly_challenge (week_start, challenge_id, selected_at, finalized_at)
SELECT '2026-06-08', id, '2026-06-08T00:05:00Z', '2026-06-15T00:05:00Z'
FROM challenge WHERE code IN (
  'EASY_COMP_MATCHES', 'NORMAL_COMP_KILLS', 'MEDIUM_COMP_MATCHES', 'HARD_COMP_HEADSHOTS', 'VERY_HARD_COMP_KILLS'
);

-- ------------------------------------------------------------------------------------------------
-- 4. Weekly scores. Natank (player 7) gets a high total_damage and every challenge completed, to
--    showcase pro-level play, but position/previous_position stay NULL: the `competitive = false`
--    flag (migration V21) keeps him out of the boss damage sum and out of challenge resolution in
--    the live app, and this fixture mirrors that by never giving him a ranking slot either.
--    total_damage = points + match_damage + regularity_bonus + team_bonus for every row.
-- ------------------------------------------------------------------------------------------------
INSERT INTO weekly_player_score (
  player_id, week_start, points, completed_challenges, match_damage, regularity_bonus, team_bonus,
  total_damage, active_days, position, previous_position, calculated_at, finalized_at
)
VALUES
  ((SELECT id FROM player WHERE display_name = 'Psilonnix'), '2026-06-08', 8000, 3, 2350, 1800, 900, 13050, 5, 1, 2, '2026-06-15T00:05:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'kikoucraft'), '2026-06-08', 4000, 2, 1850, 1200, 900, 7950, 4, 2, 1, '2026-06-15T00:05:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'NoWayToLearn'), '2026-06-08', 1500, 1, 1350, 700, 750, 4300, 3, 3, 4, '2026-06-15T00:05:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'getjfox'), '2026-06-08', 1500, 1, 850, 300, 750, 3400, 2, 4, 3, '2026-06-15T00:05:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'DuffManBzH'), '2026-06-08', 1500, 1, 850, 300, 750, 3400, 2, 5, 6, '2026-06-15T00:05:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'Izakiel'), '2026-06-08', 0, 0, 650, 300, 0, 950, 2, 6, 5, '2026-06-15T00:05:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'MDR nataNk'), '2026-06-08', 23000, 5, 2500, 1800, 0, 27300, 5, NULL, NULL, '2026-06-15T00:05:00Z', '2026-06-15T00:05:00Z');

-- ------------------------------------------------------------------------------------------------
-- 5. Challenge progress. Completed rows for every player who contributed to the points above, plus
--    a few in-progress rows for visual texture on players who did not complete anything (or
--    completed only some challenges).
-- ------------------------------------------------------------------------------------------------
INSERT INTO player_challenge_progress (
  player_id, weekly_challenge_id, current_value, target_value, completed, completed_at, calculated_at
)
VALUES
  ((SELECT id FROM player WHERE display_name = 'Psilonnix'), (SELECT wc.id FROM weekly_challenge wc JOIN challenge c ON c.id = wc.challenge_id WHERE wc.week_start = '2026-06-08' AND c.code = 'EASY_COMP_MATCHES'), 12, 12, true, '2026-06-13T20:00:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'Psilonnix'), (SELECT wc.id FROM weekly_challenge wc JOIN challenge c ON c.id = wc.challenge_id WHERE wc.week_start = '2026-06-08' AND c.code = 'NORMAL_COMP_KILLS'), 250, 250, true, '2026-06-14T18:00:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'Psilonnix'), (SELECT wc.id FROM weekly_challenge wc JOIN challenge c ON c.id = wc.challenge_id WHERE wc.week_start = '2026-06-08' AND c.code = 'MEDIUM_COMP_MATCHES'), 20, 20, true, '2026-06-14T21:00:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'kikoucraft'), (SELECT wc.id FROM weekly_challenge wc JOIN challenge c ON c.id = wc.challenge_id WHERE wc.week_start = '2026-06-08' AND c.code = 'EASY_COMP_MATCHES'), 12, 12, true, '2026-06-13T19:00:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'kikoucraft'), (SELECT wc.id FROM weekly_challenge wc JOIN challenge c ON c.id = wc.challenge_id WHERE wc.week_start = '2026-06-08' AND c.code = 'NORMAL_COMP_KILLS'), 250, 250, true, '2026-06-14T19:00:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'kikoucraft'), (SELECT wc.id FROM weekly_challenge wc JOIN challenge c ON c.id = wc.challenge_id WHERE wc.week_start = '2026-06-08' AND c.code = 'MEDIUM_COMP_MATCHES'), 14, 20, false, NULL, '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'NoWayToLearn'), (SELECT wc.id FROM weekly_challenge wc JOIN challenge c ON c.id = wc.challenge_id WHERE wc.week_start = '2026-06-08' AND c.code = 'EASY_COMP_MATCHES'), 12, 12, true, '2026-06-13T20:30:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'NoWayToLearn'), (SELECT wc.id FROM weekly_challenge wc JOIN challenge c ON c.id = wc.challenge_id WHERE wc.week_start = '2026-06-08' AND c.code = 'NORMAL_COMP_KILLS'), 180, 250, false, NULL, '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'getjfox'), (SELECT wc.id FROM weekly_challenge wc JOIN challenge c ON c.id = wc.challenge_id WHERE wc.week_start = '2026-06-08' AND c.code = 'EASY_COMP_MATCHES'), 12, 12, true, '2026-06-13T21:00:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'DuffManBzH'), (SELECT wc.id FROM weekly_challenge wc JOIN challenge c ON c.id = wc.challenge_id WHERE wc.week_start = '2026-06-08' AND c.code = 'EASY_COMP_MATCHES'), 12, 12, true, '2026-06-14T17:00:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'Izakiel'), (SELECT wc.id FROM weekly_challenge wc JOIN challenge c ON c.id = wc.challenge_id WHERE wc.week_start = '2026-06-08' AND c.code = 'EASY_COMP_MATCHES'), 9, 12, false, NULL, '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'MDR nataNk'), (SELECT wc.id FROM weekly_challenge wc JOIN challenge c ON c.id = wc.challenge_id WHERE wc.week_start = '2026-06-08' AND c.code = 'EASY_COMP_MATCHES'), 12, 12, true, '2026-06-10T20:00:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'MDR nataNk'), (SELECT wc.id FROM weekly_challenge wc JOIN challenge c ON c.id = wc.challenge_id WHERE wc.week_start = '2026-06-08' AND c.code = 'NORMAL_COMP_KILLS'), 250, 250, true, '2026-06-11T20:00:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'MDR nataNk'), (SELECT wc.id FROM weekly_challenge wc JOIN challenge c ON c.id = wc.challenge_id WHERE wc.week_start = '2026-06-08' AND c.code = 'MEDIUM_COMP_MATCHES'), 20, 20, true, '2026-06-12T20:00:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'MDR nataNk'), (SELECT wc.id FROM weekly_challenge wc JOIN challenge c ON c.id = wc.challenge_id WHERE wc.week_start = '2026-06-08' AND c.code = 'HARD_COMP_HEADSHOTS'), 200, 200, true, '2026-06-13T20:00:00Z', '2026-06-15T00:05:00Z'),
  ((SELECT id FROM player WHERE display_name = 'MDR nataNk'), (SELECT wc.id FROM weekly_challenge wc JOIN challenge c ON c.id = wc.challenge_id WHERE wc.week_start = '2026-06-08' AND c.code = 'VERY_HARD_COMP_KILLS'), 600, 600, true, '2026-06-14T20:00:00Z', '2026-06-15T00:05:00Z');

-- ------------------------------------------------------------------------------------------------
-- 6. Boss encounter. A MINOR boss (base HP 80 000) at a 75% difficulty modifier (effective HP
--    60 000) survives the six competitive players' combined 33 050 total damage (see step 4), which
--    leaves the HP bar sitting at roughly 45% for a good mid-fight screenshot. Natank's 27 300 is
--    excluded from this sum entirely, exactly like the live `DefaultBossQueryService` does for any
--    `competitive = false` player.
-- ------------------------------------------------------------------------------------------------
INSERT INTO weekly_boss_encounter (
  week_start, boss_catalog_entry_id, ruleset_version, base_hp, difficulty_modifier_percent,
  effective_hp, defeated, defeated_by_player_id, finishing_player_match_id, win_streak, finalized_at
)
SELECT
  '2026-06-08', id, 1, 80000, 75, 60000, false, NULL, NULL, 2, '2026-06-15T00:05:00Z'
FROM boss_catalog_entry WHERE code = 'DRONE_ROUILLE';
