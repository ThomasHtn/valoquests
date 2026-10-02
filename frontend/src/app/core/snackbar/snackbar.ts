import { Service, signal } from '@angular/core';
import { SnackbarMessage } from './snackbar.model';
import { SNACKBAR_DURATION_MS, SNACKBAR_ERROR_DURATION_MS } from './snackbar.constants';

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
   * Time left before the current message goes, kept while the reader holds it open.
   */
  private remainingMs = 0;

  /**
   * When the running timer started, to know how much of {@link remainingMs} it consumed.
   */
  private startedAt = 0;

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
  /**
   * Holds the current message on screen while the reader points at it or focuses it.
   */
  public pause(): void {
    if (this.current() === null || this.dismissTimer === undefined) {
      return;
    }
    clearTimeout(this.dismissTimer);
    this.dismissTimer = undefined;
    this.remainingMs = Math.max(0, this.remainingMs - (Date.now() - this.startedAt));
  }

  /**
   * Lets the current message run out the time it had left.
   */
  public resume(): void {
    if (this.current() === null || this.dismissTimer !== undefined) {
      return;
    }
    this.arm(this.remainingMs);
  }

  private enqueue(message: SnackbarMessage): void {
    // The same text already showing or queued, as a flapping connection repeats it, is dropped.
    const last = this.pending.at(-1) ?? this.current();
    if (last?.text === message.text && last.type === message.type) {
      return;
    }
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
      this.arm(message.type === 'error' ? SNACKBAR_ERROR_DURATION_MS : SNACKBAR_DURATION_MS);
    }
  }

  private arm(durationMs: number): void {
    this.remainingMs = durationMs;
    this.startedAt = Date.now();
    this.dismissTimer = setTimeout(() => {
      this.dismissTimer = undefined;
      this.showNext();
    }, durationMs);
  }
}
