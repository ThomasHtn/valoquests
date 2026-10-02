/**
 * Pages a back link can name, by path pattern, with the translation key of their name. A page left
 * out here is never offered as a way back: the link keeps its static parent instead.
 */
export const BACK_LABEL_KEYS: readonly { readonly pattern: RegExp; readonly key: string }[] = [
  { pattern: /^\/overview$/, key: 'sidebar.nav.overview' },
  { pattern: /^\/challenges$/, key: 'sidebar.nav.challenges' },
  { pattern: /^\/leaderboard$/, key: 'sidebar.nav.leaderboard' },
  { pattern: /^\/players$/, key: 'sidebar.nav.players' },
  { pattern: /^\/players\/\d+$/, key: 'playerProfile.matches.detail.back' },
  { pattern: /^\/rules$/, key: 'sidebar.nav.rules' },
];
