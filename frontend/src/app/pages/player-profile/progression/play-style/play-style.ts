import { Component, computed, inject, input } from '@angular/core';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { formatHeadshotPercentage } from '@core/players/player-format.utils';
import { AimBreakdown } from '@core/players/progression/player-progression.model';
import { Tooltip } from '@shared/tooltip/tooltip';
import { AimZone } from './play-style.model';
import { MINIMUM_OPACITY } from './play-style.constants';

/**
 * Where shots land, drawn on a range-target dummy.
 * One amber hue at varying strength: the split is a magnitude, not three unrelated things.
 */
@Component({
  selector: 'app-play-style',
  imports: [TranslatePipe, Tooltip],
  templateUrl: './play-style.html',
  styleUrl: './play-style.scss',
})
export class PlayStyle {
  /**
   * Hit breakdown from the API.
   */
  public readonly aim = input.required<AimBreakdown>();

  /**
   * Builds the figure's accessible description.
   */
  private readonly translation = inject(Translation);

  /**
   * The three zones, head first, with their share and drawn strength.
   */
  protected readonly zones = computed<readonly AimZone[]>(() => {
    const aim = this.aim();
    const shares: readonly { key: AimZone['key']; percentage: number }[] = [
      { key: 'head', percentage: aim.headPercentage },
      { key: 'body', percentage: aim.bodyPercentage },
      { key: 'legs', percentage: aim.legPercentage },
    ];
    const strongest = Math.max(...shares.map((share) => share.percentage), 0);

    return shares.map((share) => ({
      key: share.key,
      percentage: share.percentage,
      label: formatHeadshotPercentage(share.percentage, this.translation.language()),
      opacity:
        strongest === 0
          ? MINIMUM_OPACITY
          : MINIMUM_OPACITY + (1 - MINIMUM_OPACITY) * (share.percentage / strongest),
    }));
  });

  /**
   * Opacity of the head zone.
   */
  protected readonly headOpacity = computed(() => this.zones()[0].opacity);

  /**
   * Opacity of the body zone.
   */
  protected readonly bodyOpacity = computed(() => this.zones()[1].opacity);

  /**
   * Opacity of the leg zones.
   */
  protected readonly legsOpacity = computed(() => this.zones()[2].opacity);

  /**
   * Whether any hit was registered; without one, colouring the zeros would mislead.
   */
  protected readonly hasSample = computed(() => this.aim().totalShots > 0);

  /**
   * Accessible description, since the silhouette is one opaque image.
   */
  protected readonly description = computed(() =>
    this.zones()
      .map((zone) =>
        this.translation.translate('playerProfile.progression.playStyle.zoneSummary', {
          zone: this.translation.translate(`playerProfile.progression.playStyle.zone.${zone.key}`),
          value: zone.label,
        }),
      )
      .join(' '),
  );
}
