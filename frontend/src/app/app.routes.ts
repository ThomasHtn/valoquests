import { Routes } from '@angular/router';

import { landingEntryGuard } from '@core/landing/landing-entry.guard';

/**
 * Application routes.
 *
 * Each page is lazy-loaded via `loadComponent` so route-level code splitting stays automatic as
 * new pages are added. A route's `title` is a translation key rather than a literal; it is
 * resolved against the active dictionary by `TranslatedTitleStrategy`.
 *
 * Two routes share the empty path. The first matches the root URL exactly (`pathMatch: 'full'`)
 * and serves the landing page, a full-bleed doorway with no navigation chrome. Every other URL
 * falls through to the second, which activates `Shell` — the sidebar layout — and resolves the
 * page among its children. The wildcard route is one of those children on purpose, so a wrong URL
 * still lands on a page the visitor can navigate away from.
 */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'landing.title',
    canActivate: [landingEntryGuard],
    loadComponent: () => import('@pages/landing/landing').then((m) => m.Landing),
  },
  {
    path: '',
    loadComponent: () => import('@layout/shell/shell').then((m) => m.Shell),
    children: [
      {
        path: 'overview',
        title: 'overview.title',
        loadComponent: () => import('@pages/overview/overview').then((m) => m.Overview),
      },
      {
        path: 'challenges',
        title: 'overview.weeklyChallenges.title',
        loadComponent: () => import('@pages/challenges/challenges').then((m) => m.Challenges),
      },
      {
        path: 'leaderboard',
        title: 'overview.weeklyRanking.title',
        loadComponent: () => import('@pages/leaderboard/leaderboard').then((m) => m.Leaderboard),
      },
      {
        path: 'players',
        title: 'players.title',
        loadComponent: () => import('@pages/players/players').then((m) => m.Players),
      },
      {
        path: 'players/:id',
        title: 'playerProfile.title',
        loadComponent: () =>
          import('@pages/player-profile/player-profile').then((m) => m.PlayerProfile),
      },
      {
        path: 'ranking',
        title: 'ranking.title',
        loadComponent: () => import('@pages/ranking/ranking').then((m) => m.Ranking),
      },
      {
        path: 'boss',
        title: 'boss.title',
        loadComponent: () => import('@pages/boss/boss').then((m) => m.Boss),
      },
      {
        path: 'rules',
        title: 'rules.title',
        loadComponent: () => import('@pages/rules/rules').then((m) => m.Rules),
      },
      {
        path: '**',
        title: 'notFound.title',
        loadComponent: () => import('@pages/not-found/not-found').then((m) => m.NotFound),
      },
    ],
  },
];
