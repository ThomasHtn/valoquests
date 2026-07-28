import { Routes } from '@angular/router';

/**
 * Application routes.
 *
 * Each page is lazy-loaded via `loadComponent` so route-level code splitting
 * stays automatic as new pages are added.
 */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/overview/overview').then((m) => m.Overview),
  },
  {
    path: 'players',
    loadComponent: () => import('./pages/players/players').then((m) => m.Players),
  },
];
