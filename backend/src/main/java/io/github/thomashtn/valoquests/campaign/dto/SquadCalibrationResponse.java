package io.github.thomashtn.valoquests.campaign.dto;

import io.github.thomashtn.valoquests.campaign.model.CampaignTier;
import io.github.thomashtn.valoquests.campaign.model.PlayerCalibration;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * What a campaign opened right now would be sized on, shown before anything is committed.
 *
 * <p>A calibration is decided once and never revised, so this preview is the only moment an
 * operator can notice that a player's history is thinner than it looks, or that the window had to
 * shrink to cover everyone.
 *
 * @param reference    average of the players' weekly averages, floor applied
 * @param tier         bracket the reference falls in
 * @param volumeFactor factor the challenge volume targets would be scaled by
 * @param windowMonths months of history the average was read over
 * @param firstDay     first day of that window
 * @param players      what each player contributed
 */
public record SquadCalibrationResponse(
    int reference,
    CampaignTier tier,
    BigDecimal volumeFactor,
    int windowMonths,
    LocalDate firstDay,
    List<PlayerCalibration> players
) {

    /**
     * Creates the response, copying the breakdown.
     */
    public SquadCalibrationResponse {
        players = List.copyOf(players);
    }
}
