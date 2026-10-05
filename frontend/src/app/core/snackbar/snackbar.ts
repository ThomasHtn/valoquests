import { Service, signal } from '@angular/core';
import { SnackbarMessage } from './snackbar.model';
import { SNACKBAR_DURATION_MS, SNACKBAR_ERROR_DURATION_MS } from './snackbar.constants';

/**
 * Global snackbar queue: one slot on screen, so back-to-back messages each get their turn.
 */
@Service()
export class SnackbarQueue {
  /**
   * Waiting messages in arrival order, excluding {@link current}.
   */
  private readonly pending: SnackbarMessage[] = [];

  /**
   * Id of the next message.
   */
  private nextId = 0;

  /**
   * Timer counting down {@link current}, if any.
   */
  private dismissTimer: ReturnType<typeof setTimeout> | undefined;

  /**
   * Time left for the current message, kept while the reader holds it.
   */
  private remainingMs = 0;

  /**
   * Start of the running timer, to know how much of {@link remainingMs} it used.
   */
  private startedAt = 0;

  /**
   * Snackbar on screen, `null` when none.
   */
  public readonly current = signal<SnackbarMessage | null>(null);

  /**
   * Queues a success snackbar with translated text.
   */
  public success(text: string): void {
    this.enqueue({ id: this.nextId++, type: 'success', text });
  }

  /**
   * Queues an error snackbar with translated text.
   */
  public error(text: string): void {
    this.enqueue({ id: this.nextId++, type: 'error', text });
  }

  /**
   * Dismisses the current snackbar early (manual close) and shows the next one.
   */
  public dismiss(): void {
    clearTimeout(this.dismissTimer);
    this.showNext();
  }

  /**
   * Holds the current message while the reader points at or focuses it.
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
   * Lets the current message run out its remaining time.
   */
  public resume(): void {
    if (this.current() === null || this.dismissTimer !== undefined) {
      return;
    }
    this.arm(this.remainingMs);
  }

  /**
   * Queues a message, dropping a repeat of the last one, and shows it if the slot is free.
   */
  private enqueue(message: SnackbarMessage): void {
    // Drop a repeat of the last message, as a flapping connection sends.
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
   * Shows the next queued message, if any, and arms its timer.
   */
  private showNext(): void {
    const message = this.pending.shift() ?? null;

    this.current.set(message);

    if (message !== null) {
      this.arm(message.type === 'error' ? SNACKBAR_ERROR_DURATION_MS : SNACKBAR_DURATION_MS);
    }
  }

  /**
   * Starts the countdown that dismisses the current message after `durationMs`.
   */
  private arm(durationMs: number): void {
    this.remainingMs = durationMs;
    this.startedAt = Date.now();
    this.dismissTimer = setTimeout(() => {
      this.dismissTimer = undefined;
      this.showNext();
    }, durationMs);
  }
}
