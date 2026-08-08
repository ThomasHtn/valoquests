import { Routes } from '@angular/router';

/**
 * Application routes.
 *
 * Each page is lazy-loaded via `loadComponent` so route-level code splitting stays automatic as
 * new pages are added. A route's `title` is a translation key rather than a literal; it is
 * resolved against the active dictionary by `TranslatedTitleStrategy`.
 */
export const routes: Routes = [
  {
    path: '',
    title: 'overview.title',
    loadComponent: () => import('@pages/overview/overview').then((m) => m.Overview),
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
];
