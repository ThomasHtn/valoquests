import { Injectable, signal } from '@angular/core';

/**
 * Open state of the navigation drawer, shared between the panel itself and the control that
 * summons it.
 *
 * Below `lg` the navigation is a drawer, and its trigger no longer lives beside it: the burger sits
 * in the page header bar (`layout/page-header/`), which is rendered by the routed page rather than
 * by the sidebar. The two therefore need a state neither of them owns.
 *
 * The trigger element itself is held here too, so closing can return focus to the control that
 * opened the drawer without the sidebar needing a reference to a button it does not render.
 */
@Injectable({ providedIn: 'root' })
export class NavigationPanel {
  /**
   * Id of the drawer panel, referenced by the trigger's `aria-controls`.
   *
   * Declared here rather than on either component: it is the one string both sides must agree on.
   */
  public readonly panelId = 'sidebar-panel';

  /**
   * Whether the drawer is open. Meaningless from `lg` up, where the panel is a static rail that is
   * always on screen.
   */
  private readonly openState = signal(false);

  /**
   * Read-only view of {@link openState} for the components rendering the drawer and its trigger.
   */
  public readonly isOpen = this.openState.asReadonly();

  /**
   * Control that opened the drawer, refocused on close.
   */
  private trigger: HTMLElement | null = null;

  /**
   * Opens the drawer, remembering the control that asked for it.
   *
   * @param trigger - The control to return focus to once the drawer closes.
   */
  public open(trigger: HTMLElement): void {
    this.trigger = trigger;
    this.openState.set(true);
  }

  /**
   * Closes the drawer and returns focus to the control that opened it.
   *
   * Guarded on the open state because the navigation entries call this on every activation, rail
   * included, where there is no drawer to close and no focus to move. The trigger is only refocused
   * while it is still in the document: navigating from the drawer destroys the page header that
   * rendered it, and focus is then placed on the routed content by the caller instead.
   */
  public close(): void {
    if (!this.openState()) {
      return;
    }

    this.openState.set(false);

    if (this.trigger?.isConnected) {
      this.trigger.focus();
    }
  }
}
