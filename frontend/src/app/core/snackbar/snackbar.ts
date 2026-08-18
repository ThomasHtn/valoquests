import { Service, signal } from '@angular/core';

import { SnackbarMessage } from './snackbar.model';

/**
 * Milliseconds a snackbar stays on screen before it is replaced by the next queued one.
 */
export const SNACKBAR_DURATION_MS = 5_000;

/**
 * Queues and exposes the application's snackbars.
 *
 * Global rather than per-page: outcomes used to be reported inline, next to whatever triggered
 * them, but a fixed bottom-anchored snackbar has only one slot on screen. Messages are queued
 * rather than overwritten so that two commands run back to back — the backoffice's operations
 * screen runs several in sequence — each still get their own turn instead of the second erasing
 * the first before it was read.
 */
@Service()
export class SnackbarService {
  /**
   * Messages waiting to be shown, in arrival order. The one currently on screen is not in this
   * queue — it lives in {@link current}.
   */
  private readonly pending: SnackbarMessage[] = [];

  /**
   * Identity of the next queued message, incremented on every push so the display component can
   * key its timebar animation on it and have the animation restart every time.
   */
  private nextId = 0;

  /**
   * Handle of the timer currently counting down {@link current}, if any.
   */
  private dismissTimer: ReturnType<typeof setTimeout> | undefined;

  /**
   * Snackbar currently on screen, or `null` when none is.
   */
  public readonly current = signal<SnackbarMessage | null>(null);

  /**
   * Queues a success snackbar.
   *
   * @param text - Already-translated message.
   */
  public success(text: string): void {
    this.enqueue({ id: this.nextId++, type: 'success', text });
  }

  /**
   * Queues an error snackbar.
   *
   * @param text - Already-translated message.
   */
  public error(text: string): void {
    this.enqueue({ id: this.nextId++, type: 'error', text });
  }

  /**
   * Dismisses whichever snackbar is on screen and shows the next queued one, if any.
   *
   * Public so the display component can call it early — on a manual close — rather than only ever
   * waiting out the timer.
   */
  public dismiss(): void {
    clearTimeout(this.dismissTimer);
    this.showNext();
  }

  /**
   * Queues a message and starts showing it right away if none is currently on screen.
   *
   * @param message - The message to queue.
   */
  private enqueue(message: SnackbarMessage): void {
    this.pending.push(message);

    if (this.current() === null) {
      this.showNext();
    }
  }

  /**
   * Pulls the next queued message onto screen, if any, and arms its auto-dismiss timer.
   */
  private showNext(): void {
    const message = this.pending.shift() ?? null;

    this.current.set(message);

    if (message !== null) {
      this.dismissTimer = setTimeout(() => this.showNext(), SNACKBAR_DURATION_MS);
    }
  }
}
