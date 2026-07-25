-- Inserts the six Valorant accounts tracked by the application.
--
-- The Riot PUUID is intentionally left null. It will be resolved from the
-- game name and tag line during the player's first synchronization.

INSERT INTO player (
  riot_puuid,
  game_name,
  tag_line,
  display_name,
  portrait
)
VALUES
  (
    NULL,
    'Psilonnix',
    'EUW',
    'Psilonnix',
    NULL
  ),
  (
    NULL,
    'kikoucraft',
    'EUW',
    'kikoucraft',
    NULL
  ),
  (
    NULL,
    'NoWayToLearn',
    'GUEZ',
    'NoWayToLearn',
    NULL
  ),
  (
    NULL,
    'getjfox',
    '6774',
    'getjfox',
    NULL
  ),
  (
    NULL,
    'DuffManBzH',
    'EUW',
    'DuffManBzH',
    NULL
  ),
  (
    NULL,
    'Izakiel',
    '94761',
    'Izakiel',
    NULL
  ),
  (
    NULL,
    'MDR nataNk',
    '1wnl',
    'MDR nataNk',
    NULL
  );
