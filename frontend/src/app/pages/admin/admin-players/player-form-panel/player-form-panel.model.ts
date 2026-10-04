import { AdminPlayerStatus } from '@core/admin/players/admin-player.model';

/**
 * Identity submitted by the player form.
 */
export interface PlayerFormResult {
  /**
   * Riot ID game name, before the `#`.
   */
  readonly gameName: string;

  /**
   * Riot ID tag line, after the `#`.
   */
  readonly tagLine: string;

  /**
   * Agent name of the bundled avatar, `null` for none.
   */
  readonly portrait: string | null;

  /**
   * Status chosen in the form.
   */
  readonly status: AdminPlayerStatus;
}
