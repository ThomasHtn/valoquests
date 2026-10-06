import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  linkedSignal,
} from '@angular/core';

import { LucideRocket, LucideUsers } from '@lucide/angular';

import { formatFigure } from '@core/i18n/format/number-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { BaseScene } from '@pages/overview/base-scene/base-scene';
import { CountUp } from '@shared/count-up/count-up';
import { ROCKET_PART_COUNT } from '@shared/rocket/rocket-drawing.constants';

import { PREVIEW_MIN_POPULATION, PREVIEW_POPULATION_STEP } from './tour-base-preview.constants';

/**
 * The overview's base scene with population and rocket sliders the visitor drags.
 */
@Component({
  selector: 'app-tour-base-preview',
  imports: [TranslatePipe, BaseScene, CountUp, LucideRocket, LucideUsers],
  templateUrl: './tour-base-preview.html',
  styleUrl: './tour-base-preview.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TourBasePreview {
  /**
   * Population the preview opens on.
   */
  public readonly population = input.required<number>();

  /**
   * Rocket stages the preview opens on.
   */
  public readonly stagesDone = input.required<number>();

  /**
   * Population of a full campaign: the city's scale and the slider's end.
   */
  public readonly fullCampaignPopulation = input.required<number>();

  /**
   * Translation, to format the population in the current language.
   */
  private readonly translation = inject(Translation);

  /**
   * Population at the start of the slider.
   */
  protected readonly minPopulation = PREVIEW_MIN_POPULATION;

  /**
   * Population added by one slider notch.
   */
  protected readonly populationStep = PREVIEW_POPULATION_STEP;

  /**
   * Rocket stages in total, the end of the rocket slider.
   */
  protected readonly stageCount = ROCKET_PART_COUNT;

  /**
   * Population the visitor dragged to, reset when the input changes.
   */
  protected readonly shownPopulation = linkedSignal(() => this.population());

  /**
   * Rocket stages the visitor dragged to, reset when the input changes.
   */
  protected readonly shownStages = linkedSignal(() => this.stagesDone());

  /**
   * Filled share of each slider's track, as a CSS length.
   */
  protected readonly populationFill = computed(
    () =>
      `${((this.shownPopulation() - this.minPopulation) / (this.fullCampaignPopulation() - this.minPopulation)) * 100}%`,
  );

  /**
   * Filled share of the rocket slider's track, as a CSS length.
   */
  protected readonly stagesFill = computed(
    () => `${(this.shownStages() / this.stageCount) * 100}%`,
  );

  /**
   * Formats a population in the current language.
   */
  protected format(amount: number): string {
    return formatFigure(amount, this.translation.language());
  }

  /**
   * Follows the population slider.
   */
  protected onPopulation(event: Event): void {
    this.shownPopulation.set((event.target as HTMLInputElement).valueAsNumber);
  }

  /**
   * Follows the rocket slider.
   */
  protected onStages(event: Event): void {
    this.shownStages.set((event.target as HTMLInputElement).valueAsNumber);
  }
}
