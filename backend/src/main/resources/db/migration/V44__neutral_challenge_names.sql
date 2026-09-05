-- Challenge names must not spell out a number: targets are resolved per campaign (volume factor
-- and talent anchors), so "Triplé" reads wrong the week its target resolves to one win.
-- Only entries whose named number is actually scaled are renamed; fixed ones ("Sept sur sept",
-- "Deux parties", "Deux modes", "Doublé du jour", "Double Deathmatch") keep their name.

UPDATE challenge SET name = 'Premières victoires' WHERE code = 'EASY_LONG_WINS';
UPDATE challenge SET name = 'Enchaînement' WHERE code = 'NORMAL_LONG_WINS';
UPDATE challenge SET name = 'Série gagnante' WHERE code = 'HARD_LONG_WINS';
UPDATE challenge SET name = 'Victoires classées' WHERE code = 'VERY_HARD_COMP_WINS';
UPDATE challenge SET name = 'Kills du jour' WHERE code = 'DAILY_LONG_KILLS';
UPDATE challenge SET name = 'Récolte du jour' WHERE code = 'DAILY_DAY_KILLS';
UPDATE challenge SET name = 'Têtes du jour' WHERE code = 'DAILY_LONG_HEADSHOTS';
UPDATE challenge SET name = 'Têtes en Deathmatch' WHERE code = 'DAILY_DM_HEADSHOTS';
UPDATE challenge SET name = 'Moisson de têtes' WHERE code = 'DAILY_DAY_HEADSHOTS';
UPDATE challenge SET name = 'Assists du jour' WHERE code = 'DAILY_LONG_ASSISTS';
