import { GuideStep } from './admin-players.model';

/**
 * Getting-started steps, in the order the operator follows them.
 */
export const GUIDE_STEPS: readonly GuideStep[] = [
  { textKey: 'admin.players.gettingStarted.addPlayers', link: null },
  {
    textKey: 'admin.players.gettingStarted.synchronize',
    link: { route: '/admin/operations', labelKey: 'admin.players.gettingStarted.operationsLink' },
  },
  { textKey: 'admin.players.gettingStarted.share', link: null },
];
