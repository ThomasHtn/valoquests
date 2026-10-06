import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { LucideRocket } from '@lucide/angular';

import { CAMPAIGN_WEEK_COUNT } from '@core/campaign/campaign.constants';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { ROCKET_PART_COUNT } from '@shared/rocket/rocket-drawing.constants';

import { TourTrackPlanet } from './tour-campaign-track.model';
import { buildTourTrack } from './tour-campaign-track.utils';

/**
 * The campaign's ten planets in a row: evacuated ones ticked, the current one ringed.
 */
@Component({
  selector: 'app-tour-campaign-track',
  imports: [NgOptimizedImage, TranslatePipe, LucideRocket],
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

  /**
   * Weeks in a campaign, the track's length and its total label.
   */
  protected readonly weekCount = CAMPAIGN_WEEK_COUNT;

  /**
   * Rocket stages in total, for the stages label.
   */
  protected readonly stageCount = ROCKET_PART_COUNT;

  /**
   * The ten planets with their evacuated or current state.
   */
  protected readonly planets = computed<readonly TourTrackPlanet[]>(() =>
    buildTourTrack(this.weekIndex(), this.planetName()),
  );

  /**
   * Width of the evacuated stretch, from the first planet's centre.
   */
  protected readonly doneWidth = computed(
    () => `${((this.weekIndex() - 1) / this.weekCount) * 100}%`,
  );
}
