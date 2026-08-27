-- Widens the boss catalogue so a ten-week run never fights the same boss twice.
--
-- The campaign now schedules its weight classes instead of drawing them (ScoringRuleset#bossCategoryForRunWeek):
-- a run spends 2 minor, 6 standard and 2 elite weeks. With three entries per class the six standard
-- weeks had to reuse each standard boss twice. Bringing the catalogue to 6 minor / 10 standard / 6 elite
-- leaves every class enough entries to cover a whole run, and enough spare that two consecutive runs do
-- not read as the same campaign.
--
-- Same provisional content convention as V19: no artwork yet (image_url left NULL), replaceable later
-- without any structural change.
INSERT INTO boss_catalog_entry (code,name,description,category) VALUES
('SENTINELLE_FISSUREE','Sentinelle Fissurée','Une tourelle abandonnée qui tire encore, mais une fois sur trois.','MINOR'),
('MIRAGE_DE_HAVEN','Mirage de Haven','Un reflet de l''escouade qui copie ses erreurs avec un temps de retard.','MINOR'),
('CHAROGNARD_DE_SPIKE','Charognard de Spike','Une bestiole qui vit des éclats de spike et détale dès qu''on la vise.','MINOR'),
('BRISEUR_DE_HAIES','Briseur de Haies','Un béhémoth qui traverse les murs plutôt que de chercher la porte.','STANDARD'),
('CHOEUR_DES_RADIOS','Chœur des Radios','Une nuée de balises qui hurle vos positions à tout le serveur.','STANDARD'),
('ARCHITECTE_DE_SPLIT','Architecte de Split','Une entité qui redessine les couloirs entre deux rounds.','STANDARD'),
('VEUVE_DE_FRACTURE','Veuve de Fracture','Une tisseuse installée sous la faille, qui piège les rotations trop lentes.','STANDARD'),
('FOURNAISE_DE_BREEZE','Fournaise de Breeze','Un colosse de sable brûlant qui avance avec la tempête.','STANDARD'),
('SERGENT_DES_ECHOS','Sergent des Échos','Un vétéran corrompu qui commande une escouade de doubles.','STANDARD'),
('MARE_DE_LOTUS','Marée de Lotus','Une masse végétale qui referme les portes tournantes derrière vous.','STANDARD'),
('SOUVERAIN_DE_PEARL','Souverain de Pearl','Le gardien immergé d''une cité qui n''aurait jamais dû remonter.','ELITE'),
('ORAGE_DE_RADIANITE','Orage de Radianite','Une tempête consciente qui frappe là où l''escouade est la plus fragile.','ELITE'),
('PREMIER_PROTOCOLE','Premier Protocole','La toute première IA Kingdom, réveillée par ce que l''escouade a détruit.','ELITE');
