package io.github.thomashtn.valoquests.match.dto;

import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Exposes one player match in the paginated match-history API.
 *
 * <p>Carries what the match was worth to the squad alongside its Valorant statistics: without it the
 * history is a wall of numbers with no bearing on the ranking or the campaign they actually fed. The
 * amount is derived on read rather than stored, see
 * {@link io.github.thomashtn.valoquests.scoring.service.DailyOutputReader}.
 *
 * @param id                      internal player-match identifier
 * @param startedAt               instant the match started
 * @param mapName                 name of the map played
 * @param gameMode                queue the match was played in
 * @param agentName               agent the player picked
 * @param result                  outcome from the player's point of view
 * @param allyScore               rounds won by the player's team
 * @param enemyScore              rounds won by the opposing team
 * @param kills                   kills scored
 * @param deaths                  times the player died
 * @param assists                 assists credited
 * @param kda                     kills plus assists over deaths
 * @param acs                     average combat score
 * @param adr                     average damage per round
 * @param headshotPercentage      share of shots that landed on the head
 * @param competitiveTier         tier the player held for this match
 * @param valoquestsDamage        damage this match dealt to the guardian, after both multipliers;
 *     {@code 0} for a match the ruleset does not value
 * @param damageCoefficientPercent share of its base damage the match kept, {@code 100} for a day's
 *     best games and lower once the day's ladder starts reducing them; {@code 0} for an unvalued
 *     match, which never enters that ladder
 * @param streakBonusPercent      bonus the player's run of consecutive days added to this match
 * @param food                    food share of the damage
 * @param components              components share of the damage
 */
@Schema(description = "Player-centric match history entry.")
public record MatchResponse(

    Long id,
    Instant startedAt,
    String mapName,
    GameMode gameMode,
    String agentName,
    MatchResult result,
    Integer allyScore,
    Integer enemyScore,
    int kills,
    int deaths,
    int assists,
    BigDecimal kda,
    BigDecimal acs,
    BigDecimal adr,
    BigDecimal headshotPercentage,
    CompetitiveTier competitiveTier,
    int valoquestsDamage,
    int damageCoefficientPercent,
    int streakBonusPercent,
    int food,
    int components
) {
}
