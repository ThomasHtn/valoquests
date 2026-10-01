import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  linkedSignal,
} from '@angular/core';
import { LucideRocket, LucideUsers } from '@lucide/angular';

import { formatDamage } from '@core/challenges/challenge-format.utils';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { BaseScene } from '@pages/overview/base-scene/base-scene';
import { CountUp } from '@shared/count-up/count-up';
import { ROCKET_PART_COUNT } from '@shared/rocket/rocket-drawing.constants';

import { PREVIEW_MIN_POPULATION, PREVIEW_POPULATION_STEP } from './tour-base-preview.constants';

/**
 * The overview's base, with two sliders under it: the visitor drags the population and the rocket's
 * stages and watches the city grow and the launcher rise, which says "the base is the score" better
 * than any sentence. The scene is the overview's own drawing; only the sliders are the tour's.
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
   * Population a full campaign is expected to reach: the scale of the city and the slider's end.
   */
  public readonly fullCampaignPopulation = input.required<number>();

  private readonly translation = inject(Translation);

  protected readonly minPopulation = PREVIEW_MIN_POPULATION;

  protected readonly populationStep = PREVIEW_POPULATION_STEP;

  protected readonly stageCount = ROCKET_PART_COUNT;

  protected readonly shownPopulation = linkedSignal(() => this.population());

  protected readonly shownStages = linkedSignal(() => this.stagesDone());

  /**
   * Filled share of each slider's track, as a CSS length.
   */
  protected readonly populationFill = computed(
    () =>
      `${((this.shownPopulation() - this.minPopulation) / (this.fullCampaignPopulation() - this.minPopulation)) * 100}%`,
  );

  protected readonly stagesFill = computed(
    () => `${(this.shownStages() / this.stageCount) * 100}%`,
  );

  protected format(amount: number): string {
    return formatDamage(amount, this.translation.language());
  }

  protected onPopulation(event: Event): void {
    this.shownPopulation.set((event.target as HTMLInputElement).valueAsNumber);
  }

  protected onStages(event: Event): void {
    this.shownStages.set((event.target as HTMLInputElement).valueAsNumber);
  }
}
