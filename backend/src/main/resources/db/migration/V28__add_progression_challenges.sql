-- Adds challenges measuring a player against their own recent form, and retires the most extreme
-- volume challenges they replace.
--
-- The catalogue's hard tiers almost all asked for raw cumulation: 65 matches in a week, 600 kills, 40
-- Deathmatch. Those hand the single largest reward in the system to whoever has the most free time,
-- which is the opposite of what the daily diminishing returns on match damage exist to encourage, and
-- they ask the same thing of a player who is already excellent as of one who is starting out.
--
-- BASELINE conditions read: reach a metric at least `target` percent above the rate you held over the
-- four weeks before this one, over at least `minimumMatches` matches. A player with no baseline cannot
-- complete one, so a month of absence is never rewarded with an easy week.

INSERT INTO challenge (code,name,description,difficulty,category,rule_type,progress_mode,conditions_json,exclusion_group,enabled,schema_version) VALUES
('NORMAL_PROGRESS_HS','Progression au viseur','Améliorer son taux de headshots en Compétitif de 5% par rapport à ses 4 dernières semaines.','NORMAL','AIM','RATIO','BASELINE','[{"metric":"HEADSHOT_RATE","operator":"GTE","target":5,"gameMode":"COMPETITIVE","minimumMatches":8}]'::jsonb,'PROGRESS_HEADSHOT_RATE',TRUE,3),
('MEDIUM_PROGRESS_ADR','Frappe plus fort','Améliorer ses dégâts par round en Compétitif de 6% par rapport à ses 4 dernières semaines.','MEDIUM','DAMAGE','RATIO','BASELINE','[{"metric":"ADR","operator":"GTE","target":6,"gameMode":"COMPETITIVE","minimumMatches":10}]'::jsonb,'PROGRESS_ADR',TRUE,3),
('MEDIUM_PROGRESS_DM_KD','Aim en hausse','Améliorer son K/D en Deathmatch de 8% par rapport à ses 4 dernières semaines.','MEDIUM','TRAINING','RATIO','BASELINE','[{"metric":"KD","operator":"GTE","target":8,"gameMode":"DEATHMATCH","minimumMatches":10}]'::jsonb,'PROGRESS_DEATHMATCH_KD',TRUE,3),
('HARD_PROGRESS_KD','Meilleur que le mois dernier','Améliorer son K/D en Compétitif de 8% par rapport à ses 4 dernières semaines.','HARD','PERFORMANCE','RATIO','BASELINE','[{"metric":"KD","operator":"GTE","target":8,"gameMode":"COMPETITIVE","minimumMatches":12}]'::jsonb,'PROGRESS_COMPETITIVE_KD',TRUE,3),
('HARD_PROGRESS_ACS','Impact en hausse','Améliorer son score de combat par round en Compétitif de 7% par rapport à ses 4 dernières semaines.','HARD','PERFORMANCE','RATIO','BASELINE','[{"metric":"ACS","operator":"GTE","target":7,"gameMode":"COMPETITIVE","minimumMatches":12}]'::jsonb,'PROGRESS_ACS',TRUE,3),
('VERY_HARD_PROGRESS_KD','Bond en avant','Améliorer son K/D en Compétitif de 15% par rapport à ses 4 dernières semaines.','VERY_HARD','PERFORMANCE','RATIO','BASELINE','[{"metric":"KD","operator":"GTE","target":15,"gameMode":"COMPETITIVE","minimumMatches":15}]'::jsonb,'PROGRESS_COMPETITIVE_KD',TRUE,3),
('VERY_HARD_PROGRESS_ACS','Palier franchi','Améliorer son score de combat par round en Compétitif de 12% par rapport à ses 4 dernières semaines.','VERY_HARD','AIM','RATIO','BASELINE','[{"metric":"ACS","operator":"GTE","target":12,"gameMode":"COMPETITIVE","minimumMatches":15}]'::jsonb,'PROGRESS_ACS',TRUE,3)
ON CONFLICT (code) DO NOTHING;

-- Disabled rather than deleted: weekly_challenge references challenge, so a row already drawn into a
-- past week cannot be removed without rewriting that week's history. Disabling keeps every finalized
-- week readable while taking these out of every future draw.
--
-- Only the extremes go. "Play 20 competitive matches" is a reasonable weekly ask; "play 65 matches all
-- modes" and "play 40 Deathmatch" are second jobs. Re-enable any of them with:
--   UPDATE challenge SET enabled = TRUE WHERE code = '<code>';
UPDATE challenge
SET enabled = FALSE,
    updated_at = now()
WHERE code IN (
    'VERY_HARD_ANY_MATCHES',
    'VERY_HARD_COMP_MATCHES',
    'VERY_HARD_DM_MATCHES',
    'VERY_HARD_TDM_MATCHES',
    'HARD_DM_MATCHES',
    'HARD_TDM_MATCHES'
);
