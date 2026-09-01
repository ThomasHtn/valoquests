-- Refills the catalogue with challenges that are decided inside one week.
--
-- V38 took the seven progression challenges out and left every tier short, VERY_HARD worst of all
-- at nine. A tier's no-repeat cycle is as long as the tier itself, so a nine-entry tier repeats
-- inside a single ten-week run; these bring all five back to fourteen.
--
-- All of them measure how a week was played rather than how long. The catalogue's weak spot has
-- always been that its hardest tiers price free time: 600 kills, 170 000 of combat score. The three
-- shapes used here price something else.
--
--   * RATIO — a rate held across the week, over a floor of eligible matches. Playing more matches
--     moves the numerator and the denominator together, so it cannot be farmed by volume; the floor
--     is what stops one lucky game from carrying it. Read by AggregateRateCalculator as one ratio of
--     totals, never as an average of per-match ratios.
--   * COUNT_MATCHES — how many matches individually cleared a bar. Missing a game costs that game
--     and nothing else, which is what makes these safe to attempt late in the week.
--   * MAX_STREAK — consecutive matches clearing a bar, in chronological order. Matches outside the
--     declared mode are skipped rather than treated as a break, so a Deathmatch between two ranked
--     games does not end a win streak.
--
-- Thresholds are set against the two anchors the catalogue already carries — HARD_WEEKLY_KD at 1.20
-- over 15 competitive matches, HARD_DM_PERFORMANCE at 30 kills in 10 Deathmatch — and against the
-- rough equivalence ACS ~ 1.6 x ADR, so the ADR and ACS entries of one tier ask a comparable thing.
-- They are the calibration to revisit first if a tier turns out to be trivial or unreachable: an
-- UPDATE on conditions_json is enough, no code moves.
--
-- Exclusion groups are shared with the existing entries measuring the same axis
-- (COMPETITIVE_WEEKLY_KD with HARD_WEEKLY_KD), so one weekly pack never asks the same question at
-- two difficulties.

INSERT INTO challenge (code,name,description,difficulty,category,progress_mode,conditions_json,exclusion_group,enabled,schema_version) VALUES
('EASY_COMP_POSITIVE_MATCHES','Bilan positif','Terminer 5 parties compétitives avec un K/D d’au moins 1.','EASY','PERFORMANCE','COUNT_MATCHES','[{"metric":"KD","operator":"GTE","target":1.0,"gameMode":"COMPETITIVE","occurrences":5,"scope":"PER_MATCH"}]'::jsonb,'COMPETITIVE_KD_OCCURRENCES',TRUE,3),
('EASY_TDM_WINS','Échauffement gagnant','Remporter 5 Team Deathmatch.','EASY','VICTORY','SUM','[{"metric":"MATCHES_WON","operator":"GTE","target":5,"gameMode":"TEAM_DEATHMATCH"}]'::jsonb,'TEAM_DEATHMATCH_WINS',TRUE,3),
('NORMAL_WIN_STREAK','Sur la lancée','Enchaîner 3 victoires compétitives consécutives.','NORMAL','VICTORY','MAX_STREAK','[{"metric":"MATCHES_WON","operator":"GTE","target":1,"gameMode":"COMPETITIVE","streak":3,"scope":"PER_MATCH"}]'::jsonb,'COMPETITIVE_WIN_STREAK',TRUE,3),
('NORMAL_COMP_HEADSHOT_MATCHES','Viseur constant','Terminer 6 parties compétitives avec au moins 8 headshots.','NORMAL','AIM','COUNT_MATCHES','[{"metric":"HEADSHOTS","operator":"GTE","target":8,"gameMode":"COMPETITIVE","occurrences":6,"scope":"PER_MATCH"}]'::jsonb,'COMPETITIVE_HEADSHOT_OCCURRENCES',TRUE,3),
('MEDIUM_COMP_ADR','Pression constante','Maintenir 130 dégâts par round en Compétitif sur au moins 10 parties.','MEDIUM','DAMAGE','RATIO','[{"metric":"ADR","operator":"GTE","target":130,"gameMode":"COMPETITIVE","minimumMatches":10}]'::jsonb,'COMPETITIVE_WEEKLY_ADR',TRUE,3),
('MEDIUM_COMP_BIG_GAMES','Grosses sorties','Terminer 6 parties compétitives avec au moins 20 kills.','MEDIUM','PERFORMANCE','COUNT_MATCHES','[{"metric":"KILLS","operator":"GTE","target":20,"gameMode":"COMPETITIVE","occurrences":6,"scope":"PER_MATCH"}]'::jsonb,'COMPETITIVE_KILL_OCCURRENCES',TRUE,3),
('MEDIUM_COMP_HEADSHOT_MATCHES','Précision répétée','Terminer 8 parties compétitives avec au moins 12 headshots.','MEDIUM','AIM','COUNT_MATCHES','[{"metric":"HEADSHOTS","operator":"GTE","target":12,"gameMode":"COMPETITIVE","occurrences":8,"scope":"PER_MATCH"}]'::jsonb,'COMPETITIVE_HEADSHOT_OCCURRENCES',TRUE,3),
('HARD_COMP_ACS','Impact soutenu','Maintenir 250 de score de combat par round en Compétitif sur au moins 12 parties.','HARD','PERFORMANCE','RATIO','[{"metric":"ACS","operator":"GTE","target":250,"gameMode":"COMPETITIVE","minimumMatches":12}]'::jsonb,'COMPETITIVE_WEEKLY_ACS',TRUE,3),
('HARD_WIN_STREAK','Série victorieuse','Enchaîner 4 victoires compétitives consécutives.','HARD','VICTORY','MAX_STREAK','[{"metric":"MATCHES_WON","operator":"GTE","target":1,"gameMode":"COMPETITIVE","streak":4,"scope":"PER_MATCH"}]'::jsonb,'COMPETITIVE_WIN_STREAK',TRUE,3),
('VERY_HARD_WEEKLY_KD','Semaine intouchable','Maintenir un K/D d’au moins 1,40 en Compétitif sur au moins 15 parties.','VERY_HARD','PERFORMANCE','RATIO','[{"metric":"KD","operator":"GTE","target":1.4,"gameMode":"COMPETITIVE","minimumMatches":15}]'::jsonb,'COMPETITIVE_WEEKLY_KD',TRUE,3),
('VERY_HARD_COMP_ADR','Rouleau compresseur','Maintenir 165 dégâts par round en Compétitif sur au moins 12 parties.','VERY_HARD','DAMAGE','RATIO','[{"metric":"ADR","operator":"GTE","target":165,"gameMode":"COMPETITIVE","minimumMatches":12}]'::jsonb,'COMPETITIVE_WEEKLY_ADR',TRUE,3),
('VERY_HARD_WIN_STREAK','Invaincu','Enchaîner 6 victoires compétitives consécutives.','VERY_HARD','VICTORY','MAX_STREAK','[{"metric":"MATCHES_WON","operator":"GTE","target":1,"gameMode":"COMPETITIVE","streak":6,"scope":"PER_MATCH"}]'::jsonb,'COMPETITIVE_WIN_STREAK',TRUE,3),
('VERY_HARD_COMP_BIG_GAMES','Carnage répété','Terminer 8 parties compétitives avec au moins 25 kills.','VERY_HARD','PERFORMANCE','COUNT_MATCHES','[{"metric":"KILLS","operator":"GTE","target":25,"gameMode":"COMPETITIVE","occurrences":8,"scope":"PER_MATCH"}]'::jsonb,'COMPETITIVE_KILL_OCCURRENCES',TRUE,3),
('VERY_HARD_COMP_ACS_MATCHES','Niveau tenu','Terminer 8 parties compétitives avec au moins 280 de score de combat par round.','VERY_HARD','CONSISTENCY','COUNT_MATCHES','[{"metric":"ACS","operator":"GTE","target":280,"gameMode":"COMPETITIVE","occurrences":8,"scope":"PER_MATCH"}]'::jsonb,'COMPETITIVE_ACS_OCCURRENCES',TRUE,3)
ON CONFLICT (code) DO NOTHING;
