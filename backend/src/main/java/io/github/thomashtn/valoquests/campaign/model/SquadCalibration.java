package io.github.thomashtn.valoquests.campaign.model;

import io.github.thomashtn.valoquests.challenge.model.SquadLevel;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * The squad measured against itself, once, before a campaign starts.
 *
 * <p>Everything a campaign is sized by comes from here: guardian hit points, group sizes, challenge
 * rewards and challenge targets. It is computed at opening and stored on the campaign row, never
 * recomputed — a reference that drifted would resize a guardian the squad is already fighting.
 *
 * @param reference   average of the players' weekly averages, floored by the ruleset
 * @param tier        bracket the reference falls in
 * @param level     squad level the campaign plays its challenges at
 * @param windowMonths months of history the average was read over
 * @param firstDay    first day of that window
 * @param players     what each player contributed, roster order
 */
public record SquadCalibration(
    int reference,
    CampaignTier tier,
    SquadLevel level,
    int windowMonths,
    LocalDate firstDay,
    List<PlayerCalibration> players
) {

    /**
     * Creates a calibration, copying the per-player breakdown.
     *
     * @throws NullPointerException when a component is {@code null}
     */
    public SquadCalibration {
        Objects.requireNonNull(tier, "tier must not be null");
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(firstDay, "firstDay must not be null");
        players = List.copyOf(players);
    }
}
