import { Component, computed, inject, input } from '@angular/core';
import { TranslatePipe } from '@core/i18n/translate-pipe';
import { Translation } from '@core/i18n/translation';
import { formatHeadshotPercentage } from '@core/players/player-format.utils';
import { AimBreakdown } from '@core/players/player-progression.model';
import { Tooltip } from '@shared/tooltip/tooltip';
import { AimZone } from './play-style.model';
import { MINIMUM_OPACITY } from './play-style.constants';

/**
 * Where a player's shots land, drawn on a range-target dummy.
 *
 * The silhouette is the profile's one loud element and everything around it stays quiet. It is
 * treated as a shooting-range target sheet — corner ticks, hairline callouts, figures set as
 * survey dimensions — because that is the vernacular this measurement actually comes from.
 *
 * One hue, not three. The split between head, body and legs is a magnitude: three separate colors
 * would say these are three unrelated things being compared, when what the reader needs to see is
 * which zone is heaviest. So every zone is the brand amber, and only its strength varies.
 */
@Component({
  selector: 'app-play-style',
  imports: [TranslatePipe, Tooltip],
  templateUrl: './play-style.html',
})
export class PlayStyle {
  /**
   * Where the player's registered hits landed, as the API returned it.
   */
  public readonly aim = input.required<AimBreakdown>();

  /**
   * i18n service, used to build the figure's accessible description.
   */
  private readonly translation = inject(Translation);

  /**
   * The three zones, head first, each carrying its share and its drawn strength.
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
      label: formatHeadshotPercentage(share.percentage),
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
   * Whether any hit was registered at all.
   *
   * With nothing in scope every share is a zero the backend had to send, not a measurement, and
   * colouring the figure from it would claim the player never aims anywhere.
   */
  protected readonly hasSample = computed(() => this.aim().totalShots > 0);

  /**
   * Accessible description of the figure, since a silhouette is one opaque image to a reader.
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
