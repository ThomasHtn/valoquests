package io.github.thomashtn.valoquests.match.dto;

import io.github.thomashtn.valoquests.match.model.GameMode;
import io.github.thomashtn.valoquests.match.model.MatchResult;
import io.github.thomashtn.valoquests.player.model.CompetitiveTier;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Exposes everything stored about one of a tracked player's matches.
 *
 * <p>A superset of {@link MatchResponse}: same identifier and figures the history page already
 * shows, plus the breakdown a single match is worth opening for — the shot-type split behind the
 * headshot rate, the raw damage and round count, the match's duration, and every other tracked
 * player found in the same lobby.
 *
 * @param id                       internal player-match identifier, matching {@link MatchResponse#id}
 * @param startedAt                instant the match started
 * @param durationSeconds          match duration, {@code null} when Henrik did not report it
 * @param mapName                  name of the map played
 * @param gameMode                 queue the match was played in
 * @param agentName                agent the player picked
 * @param result                   outcome from the player's point of view
 * @param allyScore                rounds won by the player's team
 * @param enemyScore               rounds won by the opposing team
 * @param kills                    kills scored
 * @param deaths                   times the player died
 * @param assists                  assists credited
 * @param kda                      kills plus assists over deaths
 * @param acs                      average combat score
 * @param adr                      average damage per round
 * @param headshots                registered headshot hits
 * @param bodyshots                registered body-shot hits
 * @param legshots                 registered leg-shot hits
 * @param headshotPercentage       share of shots that landed on the head
 * @param damageDealt              total damage dealt during the match
 * @param roundsPlayed             rounds used to normalize per-round statistics
 * @param mvp                      whether the player earned the match MVP designation
 * @param competitiveTier          tier the player held for this match
 * @param valoquestsDamage         damage this match dealt to its week's boss, after the day's
 *     diminishing returns; {@code 0} for a match the ruleset does not value
 * @param damageCoefficientPercent share of its base damage the match kept, {@code 100} for a day's
 *     best games and lower once the day's ladder starts reducing them; {@code 0} for an unvalued
 *     match, which never enters that ladder
 * @param teammates                every other tracked player found in the same match, on either team
 */
@Schema(description = "Full detail of one player-centric match.")
public record MatchDetailResponse(

    Long id,
    Instant startedAt,
    Integer durationSeconds,
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
    int headshots,
    int bodyshots,
    int legshots,
    BigDecimal headshotPercentage,
    int damageDealt,
    int roundsPlayed,
    boolean mvp,
    CompetitiveTier competitiveTier,
    int valoquestsDamage,
    int damageCoefficientPercent,
    List<MatchTeammateResponse> teammates
) {

    /**
     * Creates an immutable match detail response.
     */
    public MatchDetailResponse {
        teammates = List.copyOf(teammates);
    }
}
