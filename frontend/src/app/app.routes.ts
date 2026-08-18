import { Routes } from '@angular/router';

import { adminGuard } from '@core/admin/admin.guard';
import { landingEntryGuard } from '@core/landing/landing-entry.guard';
import { tourEntryGuard } from '@core/tour/tour-entry.guard';

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
 *
 * The guided tour sits between the two, and must stay declared *before* the `Shell` route: it is
 * the landing page's continuation and renders chrome-free like it, so being resolved as one of
 * `Shell`'s children would both wrap it in the sidebar and, failing that, hand it to the wildcard.
 * The backoffice's sign-in screen is declared there for the same reason.
 *
 * The backoffice itself is reachable by URL only — nothing in the application links to it — and its
 * pages are ordinary `Shell` children: signing in swaps the sidebar's entries rather than replacing
 * the layout, so the coach stays in the same application rather than crossing into a second one.
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
    path: 'tour',
    title: 'tour.title',
    canActivate: [tourEntryGuard],
    loadComponent: () => import('@pages/tour/tour').then((m) => m.Tour),
  },
  {
    path: 'admin/login',
    title: 'admin.login.title',
    loadComponent: () => import('@pages/admin/admin-login/admin-login').then((m) => m.AdminLogin),
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
        path: 'campaign',
        title: 'campaign.title',
        loadComponent: () => import('@pages/campaign/campaign').then((m) => m.Campaign),
      },
      {
        path: 'rules',
        title: 'rules.title',
        loadComponent: () => import('@pages/rules/rules').then((m) => m.Rules),
      },
      {
        path: 'admin/operations',
        title: 'admin.operations.title',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('@pages/admin/admin-operations/admin-operations').then((m) => m.AdminOperations),
      },
      {
        path: 'admin/players',
        title: 'admin.players.title',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('@pages/admin/admin-players/admin-players').then((m) => m.AdminPlayers),
      },
      {
        path: 'admin/maintenance',
        title: 'admin.maintenance.title',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('@pages/admin/admin-maintenance/admin-maintenance').then(
            (m) => m.AdminMaintenance,
          ),
      },
      {
        path: 'admin/design-system',
        title: 'admin.designSystem.title',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('@pages/admin/admin-design-system/admin-design-system').then(
            (m) => m.AdminDesignSystem,
          ),
      },
      {
        path: '**',
        title: 'notFound.title',
        loadComponent: () => import('@pages/not-found/not-found').then((m) => m.NotFound),
      },
    ],
  },
];
