/**
 * Snackbar outcome, driving both colour and icon so colour never stands alone.
 */
export type SnackbarType = 'success' | 'error';

/**
 * Queued snackbar.
 */
export interface SnackbarMessage {
  /**
   * Keys the timebar animation so it restarts on every message.
   */
  readonly id: number;

  /**
   * Tone of the message.
   */
  readonly type: SnackbarType;

  /**
   * Translated text.
   */
  readonly text: string;
}
