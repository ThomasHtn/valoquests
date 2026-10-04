import { ListboxKeyboardControls } from './listbox-keyboard.model';

/**
 * Keyboard handling of the ARIA select-only combobox; the caller's `pick` confirms a row.
 */
export function handleListboxKeydown(
  event: KeyboardEvent,
  controls: ListboxKeyboardControls,
): void {
  const lastIndex = controls.optionCount - 1;

  switch (event.key) {
    case 'ArrowDown':
    case 'ArrowUp': {
      event.preventDefault();
      if (!controls.isOpen()) {
        controls.open();
        return;
      }
      const delta = event.key === 'ArrowDown' ? 1 : -1;
      controls.activeIndex.update((index) => Math.min(lastIndex, Math.max(0, index + delta)));
      return;
    }

    case 'Home':
    case 'End': {
      if (!controls.isOpen()) {
        return;
      }
      event.preventDefault();
      controls.activeIndex.set(event.key === 'Home' ? 0 : lastIndex);
      return;
    }

    case 'Enter':
    case ' ': {
      // Stops the trigger's native click from also firing.
      event.preventDefault();
      if (!controls.isOpen()) {
        controls.open();
        return;
      }
      controls.pick(controls.activeIndex());
      return;
    }

    case 'Escape': {
      if (controls.isOpen()) {
        event.preventDefault();
        controls.closeAndRefocus();
      }
      return;
    }

    case 'Tab': {
      // Let focus leave, but never leave the list open behind it.
      controls.close();
      return;
    }

    default:
      return;
  }
}
