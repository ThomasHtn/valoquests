import { AdminPlayerStatus } from '@core/admin/admin.model';

/**
 * Identity the operator submitted, before it is turned into a create or update request.
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
   * Agent name backing the bundled avatar, or `null` when none was chosen.
   */
  readonly portrait: string | null;

  /**
   * Status chosen in the form.
   */
  readonly status: AdminPlayerStatus;
}
