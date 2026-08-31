import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideUsers } from '@lucide/angular';

import { ColonyView } from '@core/colony/colony-view';
import { resolveColonyDeltaColorClass } from '@core/colony/colony-visual.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';

/**
 * Population tile of the overview page.
 *
 * The run's population is the one figure worth standing beside the week's fight: the colony's
 * glyph beside its headcount, doubling as the link into `/campaign` the way the retired campaign
 * summary band used to. Bare rather than framed in a card: the tile's own info bubble
 * only repeated what the campaign page already states in full one click away.
 *
 * Renders nothing at all until the colony resolves, rather than a placeholder: it is a complement
 * to the week's own boss card, not one of its states, so a failed request must not put an error
 * state beside a boss card that is otherwise fine.
 */
@Component({
  selector: 'app-population-tile',
  imports: [TranslatePipe, RouterLink, LucideUsers],
  templateUrl: './population-tile.html',
  host: { class: 'block h-full' },
  providers: [ColonyView],
})
export class PopulationTile {
  /**
   * The run, resolved into the same display-ready view models the campaign page reads.
   */
  protected readonly colony = inject(ColonyView);

  /**
   * Text color of the arrivals mark, by the direction the night moved.
   */
  protected readonly deltaColorClass = resolveColonyDeltaColorClass;
}
