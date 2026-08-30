import { Routes } from '@angular/router';

import { Shell } from '@layout/shell/shell';
import { adminGuard } from '@core/admin/admin.guard';
import { landingEntryGuard } from '@core/landing/landing-entry.guard';
import { tourEntryGuard } from '@core/tour/tour-entry.guard';
import { Campaign } from '@pages/campaign/campaign';
import { Landing } from '@pages/landing/landing';
import { Leaderboard } from '@pages/leaderboard/leaderboard';
import { NotFound } from '@pages/not-found/not-found';
import { Overview } from '@pages/overview/overview';
import { Players } from '@pages/players/players';
import { Rules } from '@pages/rules/rules';
import { Tour } from '@pages/tour/tour';
import { Week } from '@pages/week/week';

/**
 * Application routes.
 *
 * The public pages are referenced eagerly, the backoffice and the two pages that read a player's
 * progression through `loadComponent`. Route-level splitting used to be the rule here, but a
 * bundler chunk is emitted for every module two lazy pages share: the public site ended up pulling
 * around twenty-five sub-kilobyte chunks — one per shared primitive — on every screen. Request count
 * is the second heaviest term of the page's environmental footprint, so the public pages, which
 * together weigh less than the shared chunks they were dragging in, now ride in the initial bundle.
 * The exceptions carry weight nothing else needs: the profile and the two-player comparison both
 * own `chart.js` through `EvolutionChart`, and the backoffice is reachable by URL only.
 *
 * A route's `title` is a translation key rather than a literal; it is resolved against the active
 * dictionary by `TranslatedTitleStrategy`.
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
    component: Landing,
  },
  {
    path: 'tour',
    title: 'tour.title',
    canActivate: [tourEntryGuard],
    component: Tour,
  },
  {
    path: 'admin/login',
    title: 'admin.login.title',
    loadComponent: () => import('@pages/admin/admin-login/admin-login').then((m) => m.AdminLogin),
  },
  {
    path: '',
    // Imported eagerly, unlike the pages it hosts: every URL but the landing page, the tour and the
    // sign-in screen activates it, so splitting it out only bought a second round trip before
    // anything could render — and its own chunk dragged a dozen sub-kilobyte shared chunks with it.
    component: Shell,
    children: [
      {
        path: 'overview',
        title: 'overview.title',
        component: Overview,
      },
      {
        path: 'week',
        title: 'overview.weeklyChallenges.title',
        component: Week,
      },
      {
        // The boss card and the challenge board fused into one page (see design-review.md §3.2);
        // the old address stays valid, on the same pattern as `colony` → `campaign` below.
        path: 'challenges',
        redirectTo: 'week',
        pathMatch: 'full',
      },
      {
        path: 'leaderboard',
        title: 'overview.weeklyRanking.title',
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
        path: 'campaign',
        title: 'campaign.title',
        component: Campaign,
      },
      {
        path: 'campaign/history/:runNumber',
        title: 'campaignSummary.eyebrow',
        loadComponent: () =>
          import('@pages/campaign/campaign-summary/campaign-summary').then(
            (m) => m.CampaignSummary,
          ),
      },
      {
        // The colony and the campaign were two pages until they turned out to be one run; the old
        // address stays valid rather than breaking every link that already points at it.
        path: 'colony',
        redirectTo: 'campaign',
        pathMatch: 'full',
      },
      {
        path: 'rules',
        title: 'rules.title',
        component: Rules,
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
