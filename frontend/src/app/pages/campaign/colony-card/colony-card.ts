import { Component, computed, input } from '@angular/core';
import { LucideInfo } from '@lucide/angular';

import { TranslatePipe } from '@core/i18n/translate-pipe';
import { TOOLTIP_SURFACE_CLASS } from '@shared/tooltip/tooltip.constants';

/**
 * One line of an info bubble's calculation rule, a name beside the figure it resolves to.
 */
export interface ColonyCardFormulaRow {
  readonly label: string;
  readonly value: string;
}

/**
 * Padding the card wraps its projected content in, at the two densities the campaign page's stat
 * grid draws tiles at.
 */
const COMFORTABLE_PADDING = 'px-6 py-5 sm:px-8 sm:py-6';
const COMPACT_PADDING = 'px-4 py-3.5';

/**
 * One tile of the campaign page's stat grid: a title, an info bubble explaining how the figure is
 * worked out, and a body left entirely to the caller.
 *
 * Every tile shares one frame — a title row capped by the coloured rule {@link accentClass}
 * carries, and a small info button opening a hover card (title, the rule the figure is built
 * from, an optional formula table, then what the figure is for). Extracted here rather than left
 * inline three more times, now that the campaign page's stat grid needs it a dozen times over.
 *
 * The icon is projected twice — once beside the title, once again inside the bubble's own title
 * line — because a single `<ng-content>` selector can only ever claim one outlet, and duplicating
 * a two-line `<svg>` at the call site is simpler than a second content projection path.
 *
 * The info button opens on hover *and* on click: a button takes focus when clicked, and focus is
 * already one of the two triggers the peer-driven bubble below reacts to, so no extra state is
 * needed to answer both.
 */
@Component({
  selector: 'app-colony-card',
  imports: [TranslatePipe, LucideInfo],
  templateUrl: './colony-card.html',
  host: { class: 'block' },
})
export class ColonyCard {
  /**
   * Already-translated name of the figure, repeated as the info bubble's own title.
   */
  public readonly title = input.required<string>();

  /**
   * Tailwind top-border colour utility capping the tile, e.g. `border-t-accent-cyan`. Ignored
   * while {@link isHero} is set, which draws its own warm glow instead.
   */
  public readonly accentClass = input('border-t-brand-500');

  /**
   * Whether this is the page's single most important figure — population — drawn with a warm
   * glow rather than the flat surface and coloured rule every other tile wears, so it reads as
   * the page's answer rather than one category among others.
   */
  public readonly isHero = input(false);

  /**
   * Whether the tile is drawn at the stat grid's tight density, for the row of eight short
   * figures under the page's two lead rows.
   */
  public readonly isCompact = input(false);

  /**
   * Text styling of the header's {@link title}, overridable for the population hero tile's own
   * headline treatment — big and white rather than every other tile's small muted label.
   */
  public readonly titleClass = input(
    'font-mono text-2xs tracking-wide text-text-secondary uppercase',
  );

  /**
   * Already-translated sentence opening the bubble: the rule the figure is built from.
   */
  public readonly infoDescription = input.required<string>();

  /**
   * Already-translated sentence closing the bubble: what the figure is for.
   */
  public readonly infoPurpose = input.required<string>();

  /**
   * Optional key/value rows between the description and the purpose, for a figure whose bubble
   * has a second reading to state (the food tile's own consumption and efficiency, beside its
   * headline surplus).
   */
  public readonly infoFormulaRows = input<readonly ColonyCardFormulaRow[]>([]);

  /**
   * Silhouette the bubble borrows from every other hover card in the application.
   */
  protected readonly tooltipSurfaceClass = TOOLTIP_SURFACE_CLASS;

  /**
   * Padding of the tile's own frame, by {@link isCompact}.
   */
  protected readonly paddingClass = computed(() =>
    this.isCompact() ? COMPACT_PADDING : COMFORTABLE_PADDING,
  );

  /**
   * The tile's own frame: the warm hero glow, or the flat surface and coloured rule every other
   * tile wears.
   */
  protected readonly frameClass = computed(() =>
    this.isHero()
      ? 'border border-brand-500/30 bg-brand-500/8'
      : `border-t-2 ${this.accentClass()} panel`,
  );
}
