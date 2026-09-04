-- Replaces the v1 campaign machinery (run, colony, weekly boss) with the v2 rescue campaign.
--
-- The colony's morale, efficiency and tier ladder are gone, and with them every column that stored
-- them; a boss encounter becomes a campaign week, which now carries its own Sunday settlement.
-- Nothing is migrated across: the two models price the same matches differently, so a v1 row read
-- through v2 rules would report a base nobody ever played for. The tables are dropped and rebuilt.
DROP TABLE IF EXISTS colony_daily_snapshot;
DROP TABLE IF EXISTS weekly_boss_encounter;
DROP TABLE IF EXISTS boss_catalog_entry;
DROP TABLE IF EXISTS campaign_settings;
DROP TABLE IF EXISTS run;

-- The guardian catalogue, carried over from the boss catalogue with its 22 entries.
--
-- Kept as a table rather than an enum: an operator adds artwork and wording to it without a
-- deployment, and a campaign points at the row it drew so renaming one never rewrites a past week.
CREATE TABLE guardian
(
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(80)  NOT NULL UNIQUE,
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(500) NOT NULL,
    category    VARCHAR(20)  NOT NULL,
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_guardian_category CHECK (category IN ('MINOR', 'STANDARD', 'ELITE'))
);

INSERT INTO guardian (code, name, description, category)
VALUES ('SPECTRE_SILENCIEUX', 'Spectre Silencieux',
        'Une ombre furtive qui se faufile entre les bombes désamorcées.', 'MINOR'),
       ('ECHO_DERAILLE', 'Écho Déraillé', 'Un signal radio corrompu qui répète vos pires calls.', 'MINOR'),
       ('DRONE_ROUILLE', 'Drone Rouillé',
        'Un vieux drone de reconnaissance qui a perdu la moitié de ses capteurs.', 'MINOR'),
       ('SENTINELLE_FISSUREE', 'Sentinelle Fissurée',
        'Une tourelle abandonnée qui tire encore, mais une fois sur trois.', 'MINOR'),
       ('MIRAGE_DE_HAVEN', 'Mirage de Haven',
        'Un reflet de l''escouade qui copie ses erreurs avec un temps de retard.', 'MINOR'),
       ('CHAROGNARD_DE_SPIKE', 'Charognard de Spike',
        'Une bestiole qui vit des éclats de spike et détale dès qu''on la vise.', 'MINOR'),
       ('GARDIEN_RADIANT', 'Gardien Radiant',
        'Un golem d''énergie né d''un excès de Radianite mal contenue.', 'STANDARD'),
       ('COLOSSE_DE_BIND', 'Colosse de Bind',
        'Une créature de pierre qui téléporte ses coups avant que vous ne les voyiez venir.', 'STANDARD'),
       ('HYDRE_DES_CONDUITS', 'Hydre des Conduits',
        'Une bête à plusieurs têtes tapie dans les tunnels d''Ascent.', 'STANDARD'),
       ('BRISEUR_DE_HAIES', 'Briseur de Haies',
        'Un béhémoth qui traverse les murs plutôt que de chercher la porte.', 'STANDARD'),
       ('CHOEUR_DES_RADIOS', 'Chœur des Radios',
        'Une nuée de balises qui hurle vos positions à tout le serveur.', 'STANDARD'),
       ('ARCHITECTE_DE_SPLIT', 'Architecte de Split',
        'Une entité qui redessine les couloirs entre deux rounds.', 'STANDARD'),
       ('VEUVE_DE_FRACTURE', 'Veuve de Fracture',
        'Une tisseuse installée sous la faille, qui piège les rotations trop lentes.', 'STANDARD'),
       ('FOURNAISE_DE_BREEZE', 'Fournaise de Breeze',
        'Un colosse de sable brûlant qui avance avec la tempête.', 'STANDARD'),
       ('SERGENT_DES_ECHOS', 'Sergent des Échos',
        'Un vétéran corrompu qui commande une escouade de doubles.', 'STANDARD'),
       ('MARE_DE_LOTUS', 'Marée de Lotus',
        'Une masse végétale qui referme les portes tournantes derrière vous.', 'STANDARD'),
       ('OMEGA_PROTOCOLE', 'Oméga Protocole',
        'Une intelligence artificielle rebelle échappée d''un laboratoire Kingdom.', 'ELITE'),
       ('DEVOREUR_ABYSSES', 'Dévoreur d''Abysses',
        'Une entité venue des profondeurs sous-marines d''Icebox.', 'ELITE'),
       ('TITAN_DE_LA_FAILLE', 'Titan de la Faille',
        'Le gardien ultime d''une faille dimensionnelle, invoqué seulement les semaines les plus intenses.',
        'ELITE'),
       ('SOUVERAIN_DE_PEARL', 'Souverain de Pearl',
        'Le gardien immergé d''une cité qui n''aurait jamais dû remonter.', 'ELITE'),
       ('ORAGE_DE_RADIANITE', 'Orage de Radianite',
        'Une tempête consciente qui frappe là où l''escouade est la plus fragile.', 'ELITE'),
       ('PREMIER_PROTOCOLE', 'Premier Protocole',
        'La toute première IA Kingdom, réveillée par ce que l''escouade a détruit.', 'ELITE');

-- One ten-week campaign, opened by the backoffice and calibrated once.
--
-- The calibration lives here rather than being recomputed: the guardians, the groups and the
-- challenge targets all hang off the reference, and a reference that moved mid-campaign would
-- retroactively resize a guardian the squad already fought.
CREATE TABLE campaign
(
    id                        BIGSERIAL PRIMARY KEY,
    number                    INTEGER      NOT NULL UNIQUE,
    status                    VARCHAR(10)  NOT NULL,
    opened_at                 TIMESTAMPTZ  NOT NULL,
    first_week_start          DATE         NOT NULL,
    last_week_start           DATE         NOT NULL,
    closed_at                 TIMESTAMPTZ,
    stopped_on                DATE,
    roster_size               INTEGER      NOT NULL,
    reference                 INTEGER      NOT NULL,
    tier                      VARCHAR(12)  NOT NULL,
    volume_factor             NUMERIC(6, 4) NOT NULL,
    skill_anchors_json        JSONB        NOT NULL,
    calibration_window_months INTEGER      NOT NULL,
    calibration_first_day     DATE         NOT NULL,
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_campaign_status CHECK (status IN ('OPENED', 'RUNNING', 'CLOSED')),
    CONSTRAINT ck_campaign_tier CHECK (tier IN ('AMATEUR', 'NORMAL', 'CONFIRMED', 'ELITE'))
);

-- At most one campaign that is not closed. Opening a second one while the first still runs would
-- give two guardians the same week and two references the same match.
CREATE UNIQUE INDEX uk_campaign_live ON campaign ((TRUE)) WHERE status <> 'CLOSED';

-- The roster frozen at opening. Deactivating a player mid-campaign must not remove their damage.
CREATE TABLE campaign_player
(
    id          BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT      NOT NULL REFERENCES campaign (id) ON DELETE CASCADE,
    player_id   BIGINT      NOT NULL REFERENCES player (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_campaign_player UNIQUE (campaign_id, player_id)
);

-- The ten weeks, drawn in full at opening so the map exists before the first match is played.
--
-- Everything left of settled is decided at opening and never moves; everything from damage_dealt on
-- is rewritten by every replay, which is why the row is the replay's output and never its input.
CREATE TABLE campaign_week
(
    id                        BIGSERIAL PRIMARY KEY,
    campaign_id               BIGINT        NOT NULL REFERENCES campaign (id) ON DELETE CASCADE,
    week_index                INTEGER       NOT NULL,
    week_start                DATE          NOT NULL,
    planet_name               VARCHAR(60)   NOT NULL,
    category                  VARCHAR(20)   NOT NULL,
    guardian_weight           NUMERIC(4, 2) NOT NULL,
    group_weight              NUMERIC(4, 2) NOT NULL,
    guardian_id               BIGINT        NOT NULL REFERENCES guardian (id),
    guardian_hit_points       INTEGER       NOT NULL,
    wounded_count             INTEGER       NOT NULL,
    damage_dealt              INTEGER       NOT NULL DEFAULT 0,
    defeated                  BOOLEAN       NOT NULL DEFAULT FALSE,
    defeated_at               TIMESTAMPTZ,
    defeated_by_player_id     BIGINT REFERENCES player (id),
    finishing_player_match_id BIGINT REFERENCES player_match (id),
    challenge_rescued         INTEGER       NOT NULL DEFAULT 0,
    extraction_rescued        INTEGER       NOT NULL DEFAULT 0,
    food_spent                INTEGER       NOT NULL DEFAULT 0,
    components_spent          INTEGER       NOT NULL DEFAULT 0,
    limiter                   VARCHAR(12)   NOT NULL DEFAULT 'NONE',
    base_loss                 NUMERIC(14, 3) NOT NULL DEFAULT 0,
    settled                   BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_campaign_week_index UNIQUE (campaign_id, week_index),
    CONSTRAINT uk_campaign_week_start UNIQUE (campaign_id, week_start),
    CONSTRAINT ck_campaign_week_category CHECK (category IN ('MINOR', 'STANDARD', 'ELITE')),
    CONSTRAINT ck_campaign_week_limiter CHECK (limiter IN ('NONE', 'GROUP', 'FOOD', 'COMPONENTS'))
);

-- One row per day of the campaign, entirely rewritten by every replay.
CREATE TABLE campaign_daily_snapshot
(
    id                BIGSERIAL PRIMARY KEY,
    campaign_id       BIGINT         NOT NULL REFERENCES campaign (id) ON DELETE CASCADE,
    day               DATE           NOT NULL,
    damage            INTEGER        NOT NULL,
    food_gained       INTEGER        NOT NULL,
    components_gained INTEGER        NOT NULL,
    growth            NUMERIC(14, 3) NOT NULL,
    eaten             NUMERIC(14, 3) NOT NULL,
    famine_loss       NUMERIC(14, 3) NOT NULL,
    guardian_loss     NUMERIC(14, 3) NOT NULL,
    arrivals          INTEGER        NOT NULL,
    food_stock        NUMERIC(14, 3) NOT NULL,
    components_stock  NUMERIC(14, 3) NOT NULL,
    population        NUMERIC(14, 3) NOT NULL,
    presence_count    INTEGER        NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uk_campaign_daily_snapshot_day UNIQUE (campaign_id, day)
);

CREATE INDEX idx_campaign_daily_snapshot_day ON campaign_daily_snapshot (campaign_id, day);

-- What each frozen-roster operator produced on each day, rewritten by every replay.
--
-- Stored rather than recomputed on read: the weekly titles, the squad table and the contribution
-- block all read it, and each of them would otherwise re-price sixty days of matches per request.
CREATE TABLE campaign_player_day
(
    id                   BIGSERIAL PRIMARY KEY,
    campaign_id          BIGINT      NOT NULL REFERENCES campaign (id) ON DELETE CASCADE,
    player_id            BIGINT      NOT NULL REFERENCES player (id),
    day                  DATE        NOT NULL,
    damage               INTEGER     NOT NULL,
    food                 INTEGER     NOT NULL,
    components           INTEGER     NOT NULL,
    match_count          INTEGER     NOT NULL,
    reduced_match_count  INTEGER     NOT NULL,
    streak_days          INTEGER     NOT NULL,
    streak_bonus_percent INTEGER     NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_campaign_player_day UNIQUE (campaign_id, player_id, day)
);

CREATE INDEX idx_campaign_player_day_day ON campaign_player_day (campaign_id, day);
