/**
 * Outcome a snackbar communicates. Colour and icon both derive from this, so the message never
 * relies on colour alone.
 */
export type SnackbarType = 'success' | 'error';

/**
 * One snackbar queued for display.
 */
export interface SnackbarMessage {
  /**
   * Identity used to key the timebar animation so it restarts on every new message, including one
   * with the same text and type as the last.
   */
  readonly id: number;
  readonly type: SnackbarType;

  /**
   * Already-translated message text.
   */
  readonly text: string;
}
