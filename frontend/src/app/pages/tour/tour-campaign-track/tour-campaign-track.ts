import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LucideRocket } from '@lucide/angular';

import { CAMPAIGN_WEEK_COUNT } from '@core/campaign/campaign.model';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { ROCKET_PART_COUNT } from '@shared/rocket/rocket-drawing.constants';

import { TourTrackPlanet } from './tour-campaign-track.model';
import { buildTourTrack } from './tour-campaign-track.utils';

/**
 * The campaign's ten planets in a row: the evacuated ones ticked, the current one ringed and named,
 * the rest still dim. Tells the first step's "ten weeks, ten planets" at a glance.
 */
@Component({
  selector: 'app-tour-campaign-track',
  imports: [TranslatePipe, LucideRocket],
  templateUrl: './tour-campaign-track.html',
  styleUrl: './tour-campaign-track.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TourCampaignTrack {
  /**
   * One-based index of the current week.
   */
  public readonly weekIndex = input.required<number>();

  /**
   * Name of the current week's planet.
   */
  public readonly planetName = input.required<string>();

  /**
   * Rocket stages built so far.
   */
  public readonly stagesDone = input.required<number>();

  protected readonly weekCount = CAMPAIGN_WEEK_COUNT;

  protected readonly stageCount = ROCKET_PART_COUNT;

  protected readonly planets = computed<readonly TourTrackPlanet[]>(() =>
    buildTourTrack(this.weekIndex(), this.planetName()),
  );

  /**
   * Share of the track's width the evacuated stretch covers, from the first planet's centre.
   */
  protected readonly doneWidth = computed(
    () => `${((this.weekIndex() - 1) / this.weekCount) * 100}%`,
  );
}
