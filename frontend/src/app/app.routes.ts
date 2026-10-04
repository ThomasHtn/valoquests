import { Routes } from '@angular/router';

import { Shell } from '@layout/shell/shell';
import { adminGuard } from '@core/admin/session/admin.guard';
import { landingEntryGuard } from '@core/landing/landing-entry.guard';
import { tourEntryGuard } from '@core/tour/tour-entry.guard';
import { Challenges } from '@pages/challenges/challenges';
import { Landing } from '@pages/landing/landing';
import { Leaderboard } from '@pages/leaderboard/leaderboard';
import { NotFound } from '@pages/not-found/not-found';
import { Overview } from '@pages/overview/overview';
import { Players } from '@pages/players/players';

/**
 * Application routes, mostly eager: lazy pages emitted one tiny shared chunk per primitive.
 * Chrome-free routes (landing, tour, sign-in) must stay before `Shell`, whose wildcard catches all.
 */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    title: 'landing.title',
    canActivate: [landingEntryGuard],
    component: Landing,
  },
  {
    // Lazy: a one-time briefing whose scene the initial bundle does not need.
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
    // Eager: almost every URL needs it, so a split only adds a round trip.
    component: Shell,
    children: [
      {
        path: 'overview',
        title: 'overview.title',
        component: Overview,
      },
      {
        path: 'challenges',
        title: 'challenges.title',
        component: Challenges,
      },
      {
        // Old address of the challenge board, kept for existing links.
        path: 'week',
        redirectTo: 'challenges',
        pathMatch: 'full',
      },
      {
        path: 'leaderboard',
        title: 'leaderboard.title',
        component: Leaderboard,
      },
      {
        path: 'players',
        title: 'players.title',
        component: Players,
      },
      {
        path: 'players/:id',
        title: 'playerProfile.title',
        loadComponent: () =>
          import('@pages/player-profile/player-profile').then((m) => m.PlayerProfile),
      },
      {
        path: 'players/:id/matches/:matchId',
        title: 'playerProfile.matches.detail.title',
        loadComponent: () =>
          import('@pages/player-profile/match-detail/match-detail').then((m) => m.MatchDetail),
      },
      {
        // Old campaign page address, kept for existing links.
        path: 'campaign',
        redirectTo: '/overview?tab=campaign',
        pathMatch: 'full',
      },
      {
        path: 'colony',
        redirectTo: 'overview',
        pathMatch: 'full',
      },
      {
        // Lazy: a reference read once.
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
        path: 'admin/campaigns',
        title: 'admin.campaigns.title',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('@pages/admin/admin-campaigns/admin-campaigns').then((m) => m.AdminCampaigns),
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
        path: '**',
        title: 'notFound.title',
        component: NotFound,
      },
    ],
  },
];
