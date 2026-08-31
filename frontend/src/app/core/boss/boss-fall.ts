import { Injectable, signal } from '@angular/core';

/**
 * `localStorage` key under which the last week whose boss fall has been celebrated is recorded.
 */
const STORAGE_KEY = 'valo-quests.boss-fall-seen';

/**
 * Remembers which weeks' boss falls the visitor has already watched.
 *
 * A boss going down is the one thing this application exists to produce, and the model records it as
 * a boolean — `defeated` is a state, never an event. Firing the celebration on that alone would
 * replay it on every visit for the rest of the week; firing it only while the page happens to be
 * open would mean it almost never fires at all, since the killing blow lands in a match, not in a
 * browser tab.
 *
 * So the event is the *first sighting*: the first time this browser opens on a week whose boss is
 * down, the sequence plays; from then on the page shows the settled aftermath. Held per week, beside
 * the language and the week recap's own record — no accounts here, and a once-a-week moment does not
 * warrant inventing any.
 */
@Injectable({ providedIn: 'root' })
export class BossFall {
  /**
   * Monday of the last week whose fall was watched, or `null` if none ever was.
   */
  private readonly seenWeek = signal<string | null>(localStorage.getItem(STORAGE_KEY));

  /**
   * Whether a week's fall still has to be shown.
   *
   * @param weekStart - Monday identifying the week, as `YYYY-MM-DD`.
   * @returns Whether the celebration is owed.
   */
  public isUnseen(weekStart: string): boolean {
    return this.seenWeek() !== weekStart;
  }

  /**
   * Records that a week's fall has been watched.
   *
   * @param weekStart - Monday identifying the week, as `YYYY-MM-DD`.
   */
  public markSeen(weekStart: string): void {
    localStorage.setItem(STORAGE_KEY, weekStart);
    this.seenWeek.set(weekStart);
  }
}
