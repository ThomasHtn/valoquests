import {
  afterNextRender,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';

import { LucideX } from '@lucide/angular';

import { DRAWER_EXIT_FALLBACK_MS } from './drawer.constants';

/**
 * Modal panel on a native `<dialog>`, which provides the backdrop, Escape and focus trap.
 * Opens as soon as it is rendered; `[drawerLeading]` fills the header beside the close button.
 */
@Component({
  selector: 'app-drawer',
  imports: [LucideX],
  templateUrl: './drawer.html',
  styleUrl: './drawer.scss',
})
export class Drawer {
  /**
   * `end`: full-height on the trailing edge; `center`: floating at its content's height.
   */
  public readonly anchor = input<'end' | 'center'>('end');

  /**
   * `id` of the element naming the drawer, for `aria-labelledby`.
   */
  public readonly labelledBy = input.required<string>();

  /**
   * Translated accessible name of the close button.
   */
  public readonly closeLabel = input.required<string>();

  /**
   * Emitted once dismissed (close button, Escape or backdrop click).
   */
  public readonly closed = output<void>();

  /**
   * Native dialog, driven through the imperative `<dialog>` API.
   */
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  /**
   * Whether the exit animation plays; the dialog closes only once it ends.
   */
  protected readonly closing = signal(false);

  /**
   * Anchor modifier plus the global enter or exit animation of that anchor.
   */
  protected readonly dialogClass = computed(() => {
    if (this.anchor() === 'center') {
      return this.closing() ? 'drawer--center fx-modal-out' : 'drawer--center fx-modal-in';
    }

    return this.closing() ? 'drawer--end fx-sheet-out' : 'drawer--end fx-sheet-in';
  });

  /**
   * Opens the modal once rendered and closes it on backdrop clicks.
   * Bound here: the a11y lint rejects a template `(click)` on a dialog (Escape covers it).
   */
  constructor() {
    // A parent `@if` may drop the open dialog, skipping the native focus return of `close()`.
    let opener: HTMLElement | null = null;
    inject(DestroyRef).onDestroy(() => {
      if (document.activeElement === document.body) {
        opener?.focus();
      }
    });

    afterNextRender(() => {
      const dialog = this.dialog().nativeElement;
      opener = document.activeElement instanceof HTMLElement ? document.activeElement : null;
      dialog.showModal();
      // A press started inside the panel (a text selection) must not close it on release.
      let pressedOnBackdrop = false;
      dialog.addEventListener('pointerdown', (event) => {
        pressedOnBackdrop = event.target === dialog;
      });
      dialog.addEventListener('click', (event) => {
        if (pressedOnBackdrop && event.target === dialog) {
          this.close();
        }
      });
    });
  }

  /**
   * Dismisses the drawer; public so projected controls can call it through a template ref.
   */
  public close(): void {
    const dialog = this.dialog().nativeElement;
    if (this.closing() || !dialog.open) {
      return;
    }
    const animated = getComputedStyle(dialog).animationName !== 'none';
    if (!animated) {
      dialog.close();
      return;
    }
    this.closing.set(true);
    // The timer covers a browser that never reports the animation's end.
    const finish = (): void => {
      clearTimeout(fallback);
      dialog.removeEventListener('animationend', onEnd);
      dialog.close();
    };
    // Content animations bubble up too; only the panel's own counts.
    const onEnd = (event: AnimationEvent): void => {
      if (event.target === dialog) {
        finish();
      }
    };
    const fallback = setTimeout(finish, DRAWER_EXIT_FALLBACK_MS);
    dialog.addEventListener('animationend', onEnd);
  }

  /**
   * Routes Escape through the animated close instead of the platform's instant one.
   */
  protected onCancel(event: Event): void {
    event.preventDefault();
    this.close();
  }
}
