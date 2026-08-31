import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { LucideCheck } from '@lucide/angular';

import { ColonyApi } from '@core/colony/colony-api';
import { tierStepFor } from '@core/colony/colony-tier.utils';
import {
  formatMultiplier,
  formatPopulation,
  formatSignedGauge,
  formatSignedPopulation,
} from '@core/colony/colony-format.utils';
import { ColonyRunHistory } from '@core/colony/colony.model';
import { formatDateRange } from '@core/date/week-period.utils';
import { anyError, anyLoading, resourceValue } from '@core/http/resource-state.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { SnackbarService } from '@core/snackbar/snackbar';
import { PageHeader } from '@layout/page-header/page-header';
import { ResourceState } from '@shared/resource-state/resource-state';
import { PAGE_LAYOUT_CLASS } from '../../page-layout.constants';
import { TownSilhouette } from '../../overview/town-silhouette/town-silhouette';

/**
 * A figure of the summary, paired with how it moved against the campaign right before this one —
 * `null` for the very first campaign, which has nothing to compare against.
 */
interface ComparedFigure {
  readonly label: string;
  readonly value: string;
  readonly deltaLabel: string | null;
  readonly isPositive: boolean;
  readonly isNegative: boolean;
}

/**
 * End-of-campaign summary — design-review.md lot 9, the run's other end from the lifecycle lot 1
 * gives an operator: what a closed campaign is worth, read back once it can no longer change.
 *
 * No validated mock-up covers this screen. Built on the same register as the rest of the redesign
 * instead — the town silhouette lot 6 already draws, the notch-tr panels and section rules every
 * other page uses — rather than inventing a visual language of its own for one screen.
 *
 * Reads `ColonyApi.history` directly rather than through `ColonyView`: that view model's own
 * `ColonyRunView` flattens a closed run down to what the `/campaign` ledger needs (a date range and
 * a score), and this page needs the fuller record — peak, average, efficiency, materials, bosses —
 * the same response already carries.
 */
/**
 * How long the "copied" acknowledgement stands, in milliseconds.
 */
const COPY_FEEDBACK_MS = 2000;

@Component({
  selector: 'app-campaign-summary',
  imports: [TranslatePipe, PageHeader, ResourceState, RouterLink, TownSilhouette, LucideCheck],
  templateUrl: './campaign-summary.html',
  host: { class: PAGE_LAYOUT_CLASS },
})
export class CampaignSummary {
  /**
   * Data-access service backing the shared closed-runs resource.
   */
  private readonly colonyApi = inject(ColonyApi);

  /**
   * i18n service, read for the active language and for the tier's translated name.
   */
  private readonly translation = inject(Translation);

  /**
   * Used to report a clipboard the browser refused.
   */
  private readonly snackbar = inject(SnackbarService);

  /**
   * Run number asked for in the URL, read once: this page is reached from a fixed link on the
   * `/campaign` ledger, never stepped through in place.
   */
  private readonly runNumber = Number(inject(ActivatedRoute).snapshot.paramMap.get('runNumber'));

  /**
   * Reactive resource fetching every closed run, shared with `/campaign`'s own ledger.
   */
  protected readonly historyResource = this.colonyApi.history;

  protected readonly isLoading = anyLoading(this.historyResource);
  protected readonly hasError = anyError(this.historyResource);

  /**
   * Every closed run, most recent first, or `[]` while loading.
   */
  private readonly history = computed(() => resourceValue(this.historyResource, []));

  /**
   * The run this page summarizes, or `null` while loading or if the number in the URL matches
   * none — a stale or mistyped link.
   */
  protected readonly run = computed<ColonyRunHistory | null>(
    () => this.history().find((entry) => entry.runNumber === this.runNumber) ?? null,
  );

  /**
   * The campaign immediately before this one, or `null` for the first campaign — nothing to
   * compare it against.
   *
   * {@link history} is ordered most recent first, so the campaign right before this one is simply
   * the next entry past it in that same list.
   */
  private readonly previousRun = computed<ColonyRunHistory | null>(() => {
    const index = this.history().findIndex((entry) => entry.runNumber === this.runNumber);

    return index === -1 ? null : (this.history()[index + 1] ?? null);
  });

  /**
   * The step this campaign finished on, which is what the scene draws. Under a clear sky: the run is
   * closed on its best state rather than on whatever morale it ended a fight with.
   *
   * The open-ended step rather than the tier's name, so a run that ended on its fourth citadel is
   * drawn as the city it was and not as the first one (see `tierStepFor`).
   */
  protected readonly step = computed<number>(() => {
    const tier = this.run()?.tier;

    return tier ? tierStepFor(tier) : 0;
  });

  /**
   * Already-translated name of the tier this campaign finished on.
   */
  protected readonly tierName = computed(() => {
    const run = this.run();

    return run
      ? this.translation.translate(`colony.tier.${run.tier.name}`, { level: run.tier.level })
      : '';
  });

  /**
   * Already-formatted date range this campaign spanned.
   */
  protected readonly dateRangeLabel = computed(() => {
    const run = this.run();

    return run ? formatDateRange(run.firstDay, run.finalDay) : '';
  });

  /**
   * Already-formatted final score.
   */
  protected readonly scoreLabel = computed(() => {
    const run = this.run();

    return run ? formatPopulation(run.finalPopulation, this.translation.language()) : '';
  });

  /**
   * Bosses defeated over the campaign, as `"7 / 10"`.
   */
  protected readonly bossesLabel = computed(() => {
    const run = this.run();

    return run ? `${run.defeatedBosses} / ${run.bossCount}` : '';
  });

  /**
   * The campaign's secondary figures, each paired with how it moved against the one right before
   * it — peak and average population, efficiency, materials banked.
   */
  protected readonly comparedFigures = computed<readonly ComparedFigure[]>(() => {
    const run = this.run();
    if (run === null) {
      return [];
    }

    const previous = this.previousRun();
    const language = this.translation.language();

    return [
      {
        label: this.translation.translate('campaignSummary.peak'),
        value: formatPopulation(run.peakPopulation, language),
        ...this.delta(run.peakPopulation, previous?.peakPopulation, (value) =>
          formatSignedPopulation(value, language),
        ),
      },
      {
        label: this.translation.translate('campaignSummary.average'),
        value: formatPopulation(run.averagePopulation, language),
        ...this.delta(run.averagePopulation, previous?.averagePopulation, (value) =>
          formatSignedPopulation(value, language),
        ),
      },
      {
        label: this.translation.translate('colony.track.efficiency.name'),
        value: formatMultiplier(run.efficiency, language),
        ...this.delta(run.efficiency, previous?.efficiency, (value) =>
          formatSignedGauge(value, language),
        ),
      },
      {
        label: this.translation.translate('colony.track.materials.name'),
        value: formatPopulation(run.materials, language),
        ...this.delta(run.materials, previous?.materials, (value) =>
          formatSignedPopulation(value, language),
        ),
      },
    ];
  });

  /**
   * Whether the address has just been copied, which the control says in place of its own label.
   *
   * A transient acknowledgement rather than a permanent state: it reverts after a beat, because the
   * question it answers ("did that work?") stops being asked.
   */
  protected readonly linkCopied = signal(false);

  /**
   * Compares a figure against its own value on the previous campaign.
   *
   * @param value    the current campaign's own figure
   * @param previous the previous campaign's same figure, or `undefined` for the first campaign
   * @param format   formats the signed difference for display
   * @returns the comparison, or a `null` label when there is nothing to compare against
   */
  private delta(
    value: number,
    previous: number | undefined,
    format: (delta: number) => string,
  ): Pick<ComparedFigure, 'deltaLabel' | 'isPositive' | 'isNegative'> {
    if (previous === undefined) {
      return { deltaLabel: null, isPositive: false, isNegative: false };
    }

    const difference = value - previous;

    return {
      deltaLabel: format(difference),
      isPositive: difference > 0,
      isNegative: difference < 0,
    };
  }

  /**
   * Puts this page's address on the clipboard.
   *
   * The absolute URL, not the route: what leaves here is pasted into a chat, and a relative path
   * would arrive as text nobody can follow.
   *
   * Falls back to the snackbar's error line when the clipboard is refused — over plain HTTP, or in
   * a browser that withholds permission — rather than failing silently and leaving somebody
   * pasting nothing.
   */
  protected copyLink(): void {
    navigator.clipboard.writeText(window.location.href).then(
      () => {
        this.linkCopied.set(true);
        setTimeout(() => this.linkCopied.set(false), COPY_FEEDBACK_MS);
      },
      () => this.snackbar.error(this.translation.translate('campaignSummary.copyFailed')),
    );
  }
}
