-- Daily pool rework: eight entries removed, three added, sixteen left.
--
-- Two facts measured on nine months of imported matches drive this:
--
--   * The Henrik payload reports zero headshots and zero damage on every Deathmatch and every
--     Skirmish row (803 and 32 matches, no exception). Any daily built on those two statistics in
--     those modes resolves to a bar of one and is meaningless, which is what DAILY_DM_HEADSHOTS had
--     become.
--   * The squad plays Competitive and Deathmatch and almost nothing else. Team Deathmatch appears
--     on 35 of 264 player-days, so a daily requiring it is dead five days out of six, and a daily
--     requiring both a Deathmatch and a Team Deathmatch on the same day covers 15 of those 264.
--
-- The seven per-match bars filtering on COMPETITIVE_OR_UNRATED go because the squad wants no ranked
-- requirement in the daily pool. Rows are deleted rather than disabled: the pool is drawn from
-- enabled rows only, and a disabled row would keep polluting the catalogue queries.
--
-- The pool loses five entries, so the no-repeat window in DefaultWeeklyChallengeSelectionService
-- moves with it.

DELETE FROM player_challenge_progress
WHERE weekly_challenge_id IN (
    SELECT weekly_challenge.id
    FROM weekly_challenge
             JOIN challenge ON challenge.id = weekly_challenge.challenge_id
    WHERE challenge.code IN (
                             'DAILY_LONG_ACS',
                             'DAILY_LONG_ADR',
                             'DAILY_LONG_KILLS',
                             'DAILY_LONG_BIG_GAME',
                             'DAILY_LONG_POSITIVE',
                             'DAILY_LONG_ASSISTS',
                             'DAILY_WARMUP',
                             'DAILY_DM_HEADSHOTS'
        )
);

DELETE FROM weekly_challenge
WHERE challenge_id IN (
    SELECT id
    FROM challenge
    WHERE code IN (
                   'DAILY_LONG_ACS',
                   'DAILY_LONG_ADR',
                   'DAILY_LONG_KILLS',
                   'DAILY_LONG_BIG_GAME',
                   'DAILY_LONG_POSITIVE',
                   'DAILY_LONG_ASSISTS',
                   'DAILY_WARMUP',
                   'DAILY_DM_HEADSHOTS'
        )
);

DELETE FROM challenge
WHERE code IN (
               'DAILY_LONG_ACS',
               'DAILY_LONG_ADR',
               'DAILY_LONG_KILLS',
               'DAILY_LONG_BIG_GAME',
               'DAILY_LONG_POSITIVE',
               'DAILY_LONG_ASSISTS',
               'DAILY_WARMUP',
               'DAILY_DM_HEADSHOTS'
    );

-- The three additions only use what the squad actually plays: Deathmatch matches, the agent picked,
-- and assists cumulated over the day. A fourth, "20 kills in Deathmatch over the day", was written
-- and dropped: DAILY_DM_KILLS already asks it as a per-match bar, and rule 5 of docs/CHALLENGES.md
-- forbids restating a per-match target as a daily cumulative one. Names spell no number that the
-- campaign rescales, per V44.
INSERT INTO challenge (code,name,description,difficulty,category,progress_mode,conditions_json,exclusion_group,enabled,schema_version,cadence) VALUES
('DAILY_DM_ROUTINE','Deux Deathmatch','Jouer 2 Deathmatch.',NULL,'TRAINING','SUM','[{"metric":"MATCHES_PLAYED","operator":"GTE","target":2,"gameMode":"DEATHMATCH"}]'::jsonb,NULL,TRUE,3,'DAILY'),
('DAILY_DAY_ASSISTS','Appui du jour','Réaliser 6 assists dans la journée, tous modes confondus.',NULL,'SUPPORT','SUM','[{"metric":"ASSISTS","operator":"GTE","target":6,"gameMode":"ANY"}]'::jsonb,NULL,TRUE,3,'DAILY'),
('DAILY_AGENT_VARIETY','Deux agents','Jouer 2 agents différents dans la journée.',NULL,'AGENT','DISTINCT_COUNT','[{"metric":"MATCHES_PLAYED","operator":"GTE","target":2,"gameMode":"ANY","groupBy":"AGENT"}]'::jsonb,NULL,TRUE,3,'DAILY')
ON CONFLICT (code) DO NOTHING;
