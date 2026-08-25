import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LucideChevronRight } from '@lucide/angular';

import { ColonyApi } from '@core/colony/colony-api';
import { formatPopulation } from '@core/colony/colony-format.utils';
import { Colony } from '@core/colony/colony.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { resourceValue } from '@core/http/resource-state.utils';
import { ProgressBar } from '@shared/progress-bar/progress-bar';

/**
 * Compact colony readout for the overview: where the population stands, and how far into the run.
 *
 * Deliberately two figures and a bar. The overview answers "how is the week going"; the colony's
 * gauges, buildings and curve answer "how is the run going", which is a different question and has
 * its own page. This strip only says whether that page is worth opening today, and links to it.
 *
 * Renders nothing at all until the colony resolves, rather than a placeholder: it is a complement
 * to the week's own blocks, not one of them, so a failed request must not put an error state in the
 * middle of a page that is otherwise fine.
 */
@Component({
  selector: 'app-colony-summary',
  imports: [TranslatePipe, RouterLink, ProgressBar, LucideChevronRight],
  templateUrl: './colony-summary.html',
  host: { class: 'block' },
})
export class ColonySummary {
  /**
   * Data-access service backing the colony, shared with the colony page itself.
   */
  private readonly colonyApi = inject(ColonyApi);

  /**
   * i18n service resolving the summary's labels.
   */
  private readonly translation = inject(Translation);

  /**
   * The colony, or `null` while it has not resolved.
   */
  protected readonly colony = computed<Colony | null>(
    () => resourceValue(this.colonyApi.colony, null) ?? null,
  );

  /**
   * Already-formatted population and capacity.
   */
  protected readonly populationLabel = computed<string>(() => {
    const colony = this.colony();

    return colony === null ? '' : formatPopulation(colony.population, this.translation.language());
  });

  protected readonly capacityLabel = computed<string>(() => {
    const colony = this.colony();

    return colony === null ? '' : formatPopulation(colony.capacity, this.translation.language());
  });

  /**
   * How far into the run today is.
   */
  protected readonly runDayLabel = computed<string>(() => {
    const colony = this.colony();

    return colony === null
      ? ''
      : this.translation.translate('colony.runDay', {
          day: colony.runDay,
          days: colony.runDayCount,
        });
  });

  /**
   * Share of capacity the population fills.
   */
  protected readonly percentage = computed<number>(() => {
    const colony = this.colony();

    return colony === null || colony.capacity === 0
      ? 0
      : (colony.population / colony.capacity) * 100;
  });
}
