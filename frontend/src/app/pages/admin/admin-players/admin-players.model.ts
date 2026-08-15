import { AdminPlayerStatus } from '@core/admin/admin.model';

/**
 * Contents of the roster form, whether it is adding a player or editing one.
 *
 * A plain object rather than the request DTOs: the form holds strings the operator is still typing,
 * including a portrait that is empty until they fill it in, while the API takes `null` for absent.
 */
export interface PlayerFormValue {
  readonly gameName: string;
  readonly tagLine: string;
  readonly displayName: string;
  readonly portrait: string;
  readonly status: AdminPlayerStatus;
}

/**
 * An empty roster form, which is also what adding a player starts from.
 */
export const EMPTY_PLAYER_FORM: PlayerFormValue = {
  gameName: '',
  tagLine: '',
  displayName: '',
  portrait: '',
  status: 'ACTIVE',
};
