import { Service, signal } from '@angular/core';

/**
 * Drawer open state and trigger, shared by the sidebar and the page header's burger.
 */
@Service()
export class NavigationPanel {
  /**
   * Drawer id, for the trigger's `aria-controls`.
   */
  public readonly panelId = 'sidebar-panel';

  /**
   * Whether the drawer is open; meaningless from `lg` up.
   */
  private readonly openState = signal(false);

  /**
   * Read-only {@link openState}.
   */
  public readonly isOpen = this.openState.asReadonly();

  /**
   * Control that opened the drawer, refocused on close.
   */
  private trigger: HTMLElement | null = null;

  /**
   * Opens the drawer, remembering `trigger` to refocus on close.
   */
  public open(trigger: HTMLElement): void {
    this.trigger = trigger;
    this.openState.set(true);
  }

  /**
   * Closes the drawer and refocuses its trigger if still in the document; no-op when closed.
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
