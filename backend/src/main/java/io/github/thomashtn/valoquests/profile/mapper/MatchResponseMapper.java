package io.github.thomashtn.valoquests.profile.mapper;

import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.profile.dto.MatchDetailResponse;
import io.github.thomashtn.valoquests.profile.dto.MatchResponse;
import io.github.thomashtn.valoquests.profile.dto.MatchTeammateResponse;
import io.github.thomashtn.valoquests.profile.dto.SquadMatchResponse;
import io.github.thomashtn.valoquests.scoring.model.ValuedMatch;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Maps persisted player matches, with their squad value, to the match-history DTOs.
 */
@Component
public class MatchResponseMapper {

    /**
     * Stands in for a match the day's ladder did not price, so every amount reads zero.
     */
    private static final ValuedMatch UNVALUED =
        new ValuedMatch(null, null, null, null, 0, 0, 0, 0, 0, 0, 0);

    /**
     * Maps one history entry, with its value once the day's ladder priced it.
     *
     * @param playerMatch           the player's match
     * @param valuedByPlayerMatchId valued matches indexed by player-match identifier
     * @return the history entry
     */
    public MatchResponse toResponse(PlayerMatch playerMatch, Map<Long, ValuedMatch> valuedByPlayerMatchId) {
        ValuedMatch valued = valuedByPlayerMatchId.getOrDefault(playerMatch.getId(), UNVALUED);
        return new MatchResponse(
            playerMatch.getId(),
            playerMatch.getMatch().getStartedAt(),
            playerMatch.getMatch().getMapName(),
            playerMatch.getMatch().getGameMode(),
            playerMatch.getAgentName(),
            playerMatch.getResult(),
            playerMatch.allyScore(),
            playerMatch.enemyScore(),
            playerMatch.getKills(),
            playerMatch.getDeaths(),
            playerMatch.getAssists(),
            playerMatch.killDeathRatio(2),
            playerMatch.getAcs(),
            playerMatch.getAdr(),
            playerMatch.headshotPercentage(),
            playerMatch.getCompetitiveTier(),
            valued.damage(),
            valued.coefficientPercent(),
            valued.streakBonusPercent(),
            valued.food(),
            valued.components()
        );
    }

    /**
     * Maps one entry of the squad's history, tagged with the player who played it.
     *
     * @param playerMatch           the player's match
     * @param valuedByPlayerMatchId valued matches indexed by player-match identifier
     * @return the squad history entry
     */
    public SquadMatchResponse toSquadResponse(
        PlayerMatch playerMatch,
        Map<Long, ValuedMatch> valuedByPlayerMatchId
    ) {
        return new SquadMatchResponse(
            playerMatch.getPlayer().getId(),
            playerMatch.getPlayer().getDisplayName(),
            playerMatch.getPlayer().getPortrait(),
            toResponse(playerMatch, valuedByPlayerMatchId)
        );
    }

    /**
     * Maps the full detail of one match, joined with the other tracked players found in it.
     *
     * @param playerMatch           the player's match
     * @param valuedByPlayerMatchId valued matches indexed by player-match identifier
     * @param others                other tracked players' entries of the same match
     * @return the match detail
     */
    public MatchDetailResponse toDetail(
        PlayerMatch playerMatch,
        Map<Long, ValuedMatch> valuedByPlayerMatchId,
        List<PlayerMatch> others
    ) {
        ValuedMatch valued = valuedByPlayerMatchId.getOrDefault(playerMatch.getId(), UNVALUED);
        return new MatchDetailResponse(
            playerMatch.getId(),
            playerMatch.getMatch().getStartedAt(),
            playerMatch.getMatch().getDurationSeconds(),
            playerMatch.getMatch().getMapName(),
            playerMatch.getMatch().getGameMode(),
            playerMatch.getAgentName(),
            playerMatch.getResult(),
            playerMatch.allyScore(),
            playerMatch.enemyScore(),
            playerMatch.getKills(),
            playerMatch.getDeaths(),
            playerMatch.getAssists(),
            playerMatch.killDeathRatio(2),
            playerMatch.getAcs(),
            playerMatch.getAdr(),
            playerMatch.getHeadshots(),
            playerMatch.getBodyshots(),
            playerMatch.getLegshots(),
            playerMatch.headshotPercentage(),
            playerMatch.getDamageDealt(),
            playerMatch.getRoundsPlayed(),
            playerMatch.isMvp(),
            playerMatch.getCompetitiveTier(),
            valued.damage(),
            valued.coefficientPercent(),
            valued.streakBonusPercent(),
            valued.food(),
            valued.components(),
            others.stream().map(other -> toTeammate(playerMatch, other)).toList()
        );
    }

    /**
     * Maps another tracked player of the same match, flagged when on the same team.
     */
    private MatchTeammateResponse toTeammate(PlayerMatch playerMatch, PlayerMatch other) {
        boolean sameTeam = playerMatch.getTeamId() != null
            && playerMatch.getTeamId().equalsIgnoreCase(other.getTeamId());
        return new MatchTeammateResponse(
            other.getPlayer().getId(),
            other.getPlayer().getDisplayName(),
            other.getPlayer().getPortrait(),
            other.getAgentName(),
            sameTeam,
            other.getResult(),
            other.getKills(),
            other.getDeaths(),
            other.getAssists(),
            other.getAcs()
        );
    }
}
