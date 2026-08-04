-- Weekly-boss gamification: a catalogue of reusable bosses, and one row per week recording which boss
-- was drawn, the hit points and difficulty modifier frozen for that week, and how the confrontation
-- ended. Total damage dealt is deliberately not stored here: it is derived from weekly_player_score,
-- which already tracks it per player, so there is a single source of truth.
CREATE TABLE boss_catalog_entry (id BIGSERIAL PRIMARY KEY,code VARCHAR(80) NOT NULL UNIQUE,name VARCHAR(120) NOT NULL,description VARCHAR(500) NOT NULL,image_url VARCHAR(300),category VARCHAR(20) NOT NULL,enabled BOOLEAN NOT NULL DEFAULT true,created_at TIMESTAMPTZ NOT NULL DEFAULT now(),updated_at TIMESTAMPTZ NOT NULL DEFAULT now());

CREATE TABLE weekly_boss_encounter (id BIGSERIAL PRIMARY KEY,week_start DATE NOT NULL,boss_catalog_entry_id BIGINT NOT NULL REFERENCES boss_catalog_entry(id),ruleset_version INTEGER NOT NULL,base_hp INTEGER NOT NULL,difficulty_modifier_percent INTEGER NOT NULL,effective_hp INTEGER NOT NULL,defeated BOOLEAN NOT NULL DEFAULT false,defeated_by_player_id BIGINT REFERENCES player(id),finishing_player_match_id BIGINT REFERENCES player_match(id),win_streak INTEGER NOT NULL DEFAULT 0,finalized_at TIMESTAMPTZ,created_at TIMESTAMPTZ NOT NULL DEFAULT now(),updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),CONSTRAINT uk_weekly_boss_encounter_week UNIQUE(week_start));

CREATE INDEX idx_weekly_boss_encounter_boss ON weekly_boss_encounter(boss_catalog_entry_id);

-- Provisional catalogue: no real artwork yet (image_url left NULL), content to be replaced later without
-- any structural change. Category only governs base HP (80k/95k/115k, applied in code by ScoringRulesetV1);
-- selection itself cycles through the whole catalogue before repeating any entry.
INSERT INTO boss_catalog_entry (code,name,description,category) VALUES
('SPECTRE_SILENCIEUX','Spectre Silencieux','Une ombre furtive qui se faufile entre les bombes désamorcées.','MINOR'),
('ECHO_DERAILLE','Écho Déraillé','Un signal radio corrompu qui répète vos pires calls.','MINOR'),
('DRONE_ROUILLE','Drone Rouillé','Un vieux drone de reconnaissance qui a perdu la moitié de ses capteurs.','MINOR'),
('GARDIEN_RADIANT','Gardien Radiant','Un golem d''énergie né d''un excès de Radianite mal contenue.','STANDARD'),
('COLOSSE_DE_BIND','Colosse de Bind','Une créature de pierre qui téléporte ses coups avant que vous ne les voyiez venir.','STANDARD'),
('HYDRE_DES_CONDUITS','Hydre des Conduits','Une bête à plusieurs têtes tapie dans les tunnels d''Ascent.','STANDARD'),
('OMEGA_PROTOCOLE','Oméga Protocole','Une intelligence artificielle rebelle échappée d''un laboratoire Kingdom.','ELITE'),
('DEVOREUR_ABYSSES','Dévoreur d''Abysses','Une entité venue des profondeurs sous-marines d''Icebox.','ELITE'),
('TITAN_DE_LA_FAILLE','Titan de la Faille','Le gardien ultime d''une faille dimensionnelle, invoqué seulement les semaines les plus intenses.','ELITE');
