package io.github.thomashtn.valoquests.profile.dto;

import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Exposes one player match in the paginated match-history API.
 *
 * <p>Also carries what the match was worth to the squad, derived on read by
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
 * @param kd                      kills over deaths, the kill total when deathless, as challenges read it
 * @param acs                     average combat score
 * @param adr                     average damage per round
 * @param headshotPercentage      share of shots that landed on the head, {@code null} without shot data
 * @param competitiveTier         tier the player held for this match
 * @param valoquestsDamage        guardian damage after both multipliers, {@code 0} when not valued
 * @param damageCoefficientPercent share of base damage kept after the day's ladder, {@code 0} when not valued
 * @param streakBonusPercent      bonus the player's days played this week added to this match
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
    BigDecimal kd,
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
