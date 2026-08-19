import { Component, input } from '@angular/core';

/**
 * Thin diamond-tipped rule separating two blocks of a page.
 *
 * The direction's separator: a solid diamond at the leading edge where the rule is strongest, a
 * hollow one where it has faded out. Takes an optional trailing {@link label} — a running total or
 * a stat qualifying the block below — which is the one part of the divider that is not decorative
 * and therefore stays exposed to assistive technology.
 */
@Component({
  selector: 'app-section-divider',
  template: `
    <span aria-hidden="true" class="size-2 shrink-0 rotate-45 bg-brand-500"></span>
    <span
      aria-hidden="true"
      class="h-px flex-1 bg-linear-to-r from-brand-500/75 to-brand-500/10"
    ></span>
    @if (label()) {
      <!-- Truncated rather than left to wrap: a long label (a campaign tally, say) wrapping to a
           second line on a narrow viewport strands the trailing diamond above it, disconnected
           from the tail end it's meant to mark. Ellipsis keeps it on the rule's single line and
           never past the container either. -->
      <span
        class="tracking-label min-w-0 truncate font-mono text-2xs font-medium text-text-muted uppercase"
      >
        {{ label() }}
      </span>
    }
    <span aria-hidden="true" class="size-2 shrink-0 rotate-45 border border-brand-500/70"></span>
  `,
  // The vertical margin rides on the component rather than on every call site: all thirty-odd of
  // them set the same `my-2`, which is not a per-page decision but part of what a divider is.
  host: { class: 'my-2 flex items-center gap-3.5' },
})
export class SectionDivider {
  /**
   * Already-translated text shown between the rule and its trailing diamond, when the divider
   * carries one.
   */
  public readonly label = input('');
}
