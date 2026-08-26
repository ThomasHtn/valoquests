import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ColonyView } from '@core/colony/colony-view';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { ColonyResourceBand } from '@shared/colony-resource-band/colony-resource-band';
import { BLOCK_TOOLTIP_DELAY_MS, Tooltip } from '@shared/tooltip/tooltip';

/**
 * Compact readout of the run in progress for the overview.
 *
 * The campaign page's own resource band, at half the size: the population the run scores, and the
 * three rails that set it. The same block rather than a summary of its own — the two screens say the
 * same thing about the same run, and drawn from two sets of markup they had already drifted into two
 * readings of it. What the band cannot carry here is what lives under a pointer: the rails' cards are
 * dropped, since the whole readout is a link to the page that states them in full (see `compact`).
 *
 * Renders nothing at all until the colony resolves, rather than a placeholder: it is a complement to
 * the week's own blocks, not one of them, so a failed request must not put an error state in the
 * middle of a page that is otherwise fine.
 */
@Component({
  selector: 'app-colony-summary',
  imports: [TranslatePipe, RouterLink, ColonyResourceBand, Tooltip],
  templateUrl: './colony-summary.html',
  // Transparent host: the section itself becomes the flex item of the overview's left column, so
  // it sits at its natural height and leaves the rest of the column to the progress panel below.
  host: { class: 'contents' },
  providers: [ColonyView],
})
export class ColonySummary {
  /**
   * The run, resolved into the same display-ready view models the campaign page reads.
   */
  protected readonly colony = inject(ColonyView);

  /**
   * i18n service resolving the one label the block writes itself.
   */
  private readonly translation = inject(Translation);

  /**
   * How long the pointer rests on the block before its tooltip opens.
   */
  protected readonly tooltipDelayMs = BLOCK_TOOLTIP_DELAY_MS;

  /**
   * How far into the run today is, `Jour 3 / 71`.
   */
  protected readonly runDayLabel = computed<string>(() => {
    const colony = this.colony.colony();

    return colony === null
      ? ''
      : this.translation.translate('colony.runDay', {
          day: colony.runDay,
          days: colony.runDayCount,
        });
  });
}
