-- Two written grids per challenge, one squad level per campaign, and the end of automatic scaling.
--
-- Targets used to be computed at draw time from nine months of history: a volume factor bounded to
-- [0.4, 3.0] for cumulative targets, and a per-match talent anchor for every bar. The result was a
-- catalogue whose numbers nobody could read — the same challenge showed twelve kills to one squad
-- and thirty to another — and a squad at the lower bound saw every daily objective divided by two
-- and a half, which one deathmatch settled.
--
-- The catalogue now carries both numbers, written by hand: conditions_json for a squad playing
-- regularly, expert_conditions_json for one that clears it without effort. A campaign freezes which
-- side it plays, exactly as it already freezes its reference, and the draw copies that grid onto the
-- selection. Nothing is computed any more.
--
-- Existing rows are given their own grid as the expert one: V47 rewrites the whole catalogue right
-- after, so the value only has to satisfy the NOT NULL between the two migrations.

ALTER TABLE challenge
    ADD COLUMN expert_conditions_json JSONB;

UPDATE challenge SET expert_conditions_json = conditions_json;

ALTER TABLE challenge
    ALTER COLUMN expert_conditions_json SET NOT NULL;

ALTER TABLE campaign
    ADD COLUMN squad_level VARCHAR(12) NOT NULL DEFAULT 'REFERENCE',
    ADD CONSTRAINT ck_campaign_squad_level CHECK (squad_level IN ('REFERENCE', 'EXPERT')),
    DROP COLUMN volume_factor,
    DROP COLUMN skill_anchors_json;

ALTER TABLE campaign
    ALTER COLUMN squad_level DROP DEFAULT;
