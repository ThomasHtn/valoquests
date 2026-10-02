-- Aligns the challenge catalogue on the interface's vocabulary.
--
-- The application says "match" everywhere, so the descriptions stop saying "partie". The order of the
-- replacements matters: "une partie" first (the article changes gender), then the plural, then the
-- singular. ChallengeDescriptionResolver still singularizes "1 matchs" into "1 match".
--
-- "Escarmouche" is the French name of Skirmish: the Team Deathmatch challenges that carried it are
-- renamed after their own mode, and the Skirmish win takes it. "Skirmish 2v2" stays as written.

UPDATE challenge
SET description = replace(
        replace(replace(description, 'une partie', 'un match'), 'parties', 'matchs'),
        'partie', 'match');

UPDATE challenge SET name = 'Match sérieux' WHERE code = 'DAILY_ONE_LONG';
UPDATE challenge SET name = 'Routine Team Deathmatch' WHERE code = 'EASY_TDM_ROUTINE';
UPDATE challenge SET name = 'Victoire en Team Deathmatch' WHERE code = 'DAILY_TDM_WIN';
UPDATE challenge SET name = 'Victoires en Team Deathmatch' WHERE code = 'MEDIUM_TDM_WINS';
UPDATE challenge SET name = 'Dégâts en Team Deathmatch' WHERE code = 'DAILY_TDM_DAMAGE';
UPDATE challenge SET name = 'Escarmouche gagnée' WHERE code = 'DAILY_SKIRMISH_WIN';
