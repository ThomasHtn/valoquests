import { Directive, input } from '@angular/core';

import { NavChipVariant } from './nav-chip.model';

/**
 * Square navigation chip (tour controls, back links), kept apart from notched `appButton` actions.
 * Owns its height so every chip matches (`styles/components/nav-chip.css`); positioning stays with the caller.
 */
@Directive({
  selector: '[appNavChip]',
  host: {
    class: 'nav-chip focus-ring press',
    '[class]': '"nav-chip--" + appNavChip()',
  },
})
export class NavChip {
  /**
   * Chip variant; the transform lets a bare `appNavChip` (empty string) mean `outline`.
   */
  public readonly appNavChip = input('outline' as NavChipVariant, {
    transform: (variant: NavChipVariant | '') => variant || 'outline',
  });
}
