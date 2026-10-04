/**
 * Placeholder initial when a match has no agent name.
 */
export const UNKNOWN_AGENT_INITIAL = '?';

/**
 * Portraits under `public/agents`, by name lowercased without punctuation (`"kayo"`).
 */
export const AGENT_IMAGE_IDS: ReadonlySet<string> = new Set([
  'astra',
  'breach',
  'brimstone',
  'chamber',
  'clove',
  'cypher',
  'deadlock',
  'fade',
  'gekko',
  'harbor',
  'iso',
  'jett',
  'kayo',
  'killjoy',
  'miks',
  'neon',
  'omen',
  'phoenix',
  'raze',
  'reyna',
  'sage',
  'skye',
  'sova',
  'tejo',
  'veto',
  'viper',
  'vyse',
  'waylay',
  'yoru',
]);

/**
 * Map images under `public/maps`, by exact Valorant map name.
 */
export const MAP_IMAGE_FILES: ReadonlySet<string> = new Set([
  'Abyss',
  'Ascent',
  'Bind',
  'Breeze',
  'Corrode',
  'Fracture',
  'Haven',
  'Icebox',
  'Lotus',
  'Pearl',
  'Split',
  'Summit',
  'Sunset',
]);
