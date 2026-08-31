import { Injectable, signal } from '@angular/core';

import { STORAGE_KEY } from './week-recap-dismissal.constants';

/**
 * Remembers which week's recap the visitor has already closed.
 *
 * The recap is the squad's weekly appointment: it opens the overview on Monday, states how the week
 * that just closed went, and steps aside once read. Whether it has been read is a preference of this
 * browser, held beside the language and the guided tour's own flag — there are no accounts here, and
 * a weekly panel does not warrant inventing any.
 *
 * Signal-backed rather than reading storage on every check: the panel closes without a reload, so
 * the value has to be reactive. Storage stays the source of truth across visits; the signal is the
 * copy this session reads.
 */
@Injectable({ providedIn: 'root' })
export class WeekRecapDismissal {
  /**
   * Monday of the last week whose recap was closed, or `null` if none ever was.
   */
  private readonly dismissedWeek = signal<string | null>(localStorage.getItem(STORAGE_KEY));

  /**
   * Whether a given week's recap has already been closed on this browser.
   *
   * @param weekStart - Monday identifying the closed week, as `YYYY-MM-DD`.
   * @returns Whether that week's recap has been dismissed.
   */
  public isDismissed(weekStart: string): boolean {
    return this.dismissedWeek() === weekStart;
  }

  /**
   * Records that a week's recap has been closed.
   *
   * Overwrites rather than accumulates: only the most recent week can be on screen, so the previous
   * entry has nothing left to answer for.
   *
   * @param weekStart - Monday identifying the closed week, as `YYYY-MM-DD`.
   */
  public dismiss(weekStart: string): void {
    localStorage.setItem(STORAGE_KEY, weekStart);
    this.dismissedWeek.set(weekStart);
  }
}
