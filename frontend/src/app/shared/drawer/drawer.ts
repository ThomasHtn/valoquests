import { afterNextRender, Component, ElementRef, input, output, viewChild } from '@angular/core';
import { LucideX } from '@lucide/angular';

/**
 * Right-anchored drawer: the campaign's week detail, the backoffice player form.
 *
 * Built on the native `<dialog>` element, so modality, the backdrop, Escape to dismiss and the
 * focus trap all come from the platform and this component only owns opening it, closing it and
 * reporting the dismissal back. The margin utilities override the user-agent's centering
 * (`margin: auto` on all four sides) to pin it to the viewport's right edge at full height, and
 * `max-h-dvh` overrides the default `max-height: calc(100% - 6px - 2em)` that would otherwise
 * leave it floating short of the top and bottom.
 *
 * Both drawers had this same element, the same class run, the same imperative wiring and the same
 * framed close button written out separately. What differs between them is only what they put in
 * the header row beside that button — a title, or a stepper — which is what `[drawerLeading]` is
 * for. A drawer is rendered only in response to the reader opening it, so there is no state where
 * it should sit closed and it shows itself as soon as it is in the DOM.
 */
@Component({
  selector: 'app-drawer',
  imports: [LucideX],
  templateUrl: './drawer.html',
})
export class Drawer {
  /**
   * `id` of the element naming the drawer, for `aria-labelledby`.
   */
  public readonly labelledBy = input.required<string>();

  /**
   * Already-translated accessible name of the close button.
   */
  public readonly closeLabel = input.required<string>();

  /**
   * Emitted once the drawer has been dismissed, by any means the platform offers (the close
   * button, Escape, or a click on the backdrop).
   */
  public readonly closed = output<void>();

  /**
   * The drawer element itself, needed to drive it through the imperative `<dialog>` API.
   */
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  /**
   * Opens the drawer as a modal as soon as it is in the DOM, and wires dismissal by clicking the
   * backdrop.
   *
   * That last listener is bound here rather than as a template `(click)`: a `<dialog>`'s backdrop
   * is a pseudo-element with no node of its own, so the click surfaces on the dialog itself, and a
   * click handler on an element that is not itself a control is exactly what the template
   * accessibility rules reject — rightly, except that here the keyboard equivalent is Escape,
   * which the platform already handles.
   */
  constructor() {
    afterNextRender(() => {
      const dialog = this.dialog().nativeElement;
      dialog.showModal();
      dialog.addEventListener('click', (event) => {
        if (event.target === dialog) {
          dialog.close();
        }
      });
    });
  }

  /**
   * Dismisses the drawer. Public so a projected control can close it through a template reference
   * (`<app-drawer #drawer>` … `(click)="drawer.close()"`). The `close` event it fires is what
   * notifies the host, so this path and the platform's own (Escape) report through one channel.
   */
  public close(): void {
    this.dialog().nativeElement.close();
  }
}
