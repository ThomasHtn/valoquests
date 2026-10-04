import { Directive, computed, input } from '@angular/core';
import { NavChipVariant } from './nav-chip.model';
import { VARIANT_CLASS } from './nav-chip.constants';

/**
 * Square navigation chip (tour controls, back links), kept apart from notched `appButton` actions.
 * Owns its height so every chip matches; positioning stays with the caller.
 */
@Directive({
  selector: '[appNavChip]',
  host: {
    class:
      'focus-ring tracking-label inline-flex h-11 cursor-pointer items-center gap-2 font-mono text-xs uppercase transition-colors motion-safe:active:scale-[0.96]',
    '[class]': 'variantClass()',
  },
})
export class NavChip {
  /**
   * Chip variant; the transform lets a bare `appNavChip` (empty string) mean `outline`.
   */
  public readonly appNavChip = input('outline' as NavChipVariant, {
    transform: (variant: NavChipVariant | '') => variant || 'outline',
  });

  /**
   * Tailwind classes of the current variant.
   */
  protected readonly variantClass = computed(() => VARIANT_CLASS[this.appNavChip()]);
}
