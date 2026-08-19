import { Directive, computed, input } from '@angular/core';

/**
 * Weight a navigation chip carries: the bordered default, or the one filled control that moves the
 * reader forward (the guided tour's next/finish).
 */
export type NavChipVariant = 'outline' | 'solid';

const VARIANT_CLASS: Record<NavChipVariant, string> = {
  outline:
    'border border-text-primary/15 px-4 font-medium text-text-muted hover:bg-brand-500/8 hover:text-text-primary disabled:cursor-default disabled:opacity-35 disabled:hover:bg-transparent disabled:hover:text-text-muted',
  solid: 'bg-brand-500 px-5 font-bold text-surface-950 hover:bg-brand-400',
};

/**
 * The "way back / way on" chip: the guided tour's skip, previous and next controls, the player
 * profile's link back to the registry, the rulebook's link into the tour.
 *
 * Deliberately not an `appButton` variant, and square-edged where those are notched: this family
 * reads as navigation rather than as an action, and folding the two together would blur a
 * distinction the direction makes on purpose (see `button.ts`).
 *
 * Unlike `appButton`, this one owns its own height. Every call site had written the chrome out by
 * hand and they had drifted to two different heights for the same affordance — the tour's controls
 * one step taller than the back link on the profile it sends you to. The height is part of what
 * makes these one family, so it is not left to the call site; anything positional (`ml-auto`,
 * `w-fit`) still is.
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
   * Which treatment this chip renders.
   *
   * Transformed rather than plainly defaulted so the directive can be applied bare (`appNavChip`),
   * which is what most call sites want: written that way the attribute's value is the empty
   * string, not an absent one.
   */
  public readonly appNavChip = input('outline' as NavChipVariant, {
    transform: (variant: NavChipVariant | '') => variant || 'outline',
  });

  /**
   * Resolved Tailwind classes for the current variant.
   */
  protected readonly variantClass = computed(() => VARIANT_CLASS[this.appNavChip()]);
}
