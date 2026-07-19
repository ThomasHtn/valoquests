package io.github.thomashtn.valorant.tracker.henrik.mapper;

import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchPlayer;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchTeam;
import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.github.thomashtn.valorant.tracker.match.model.MatchResult;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.model.CompetitiveTier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Maps Henrik match transport objects to persistence entities. */
@Component
public class HenrikMatchMapper {

    /** Maps shared match metadata to the persisted match entity. */
    public ValorantMatch toValorantMatch(
        HenrikMatchData source,
        Season season
    ) {
        HenrikMatchMetadata metadata = requireMetadata(source);
        GameMode gameMode = toGameMode(metadata.queue());

        ValorantMatch target = new ValorantMatch();
        target.setExternalMatchId(metadata.matchId());
        target.setSeason(season);
        target.setStartedAt(metadata.startedAt());
        target.setDurationSeconds(toDurationSeconds(
            metadata.gameLengthInMilliseconds()
        ));
        target.setMapId(
            metadata.map() == null ? null : metadata.map().id()
        );
        target.setMapName(
            metadata.map() == null || metadata.map().name() == null
                ? "Unknown"
                : metadata.map().name()
        );
        target.setQueueId(
            metadata.queue() == null ? null : metadata.queue().id()
        );
        target.setGameMode(gameMode);
        target.setRedScore(teamScore(source, "Red"));
        target.setBlueScore(teamScore(source, "Blue"));
        return target;
    }

    /** Maps the tracked player's statistics for one persisted match. */
    public PlayerMatch toPlayerMatch(
        HenrikMatchData source,
        HenrikMatchPlayer sourcePlayer,
        Player player,
        ValorantMatch match
    ) {
        HenrikMatchPlayer.HenrikPlayerStats stats =
            sourcePlayer.stats();
        HenrikMatchTeam team = findTeam(
            source,
            sourcePlayer.teamId()
        );

        int rounds = roundsPlayed(team);
        boolean roundBasedMode = supportsRoundAverages(
            match.getGameMode()
        );

        PlayerMatch target = new PlayerMatch();
        target.setPlayer(player);
        target.setMatch(match);
        target.setTeamId(sourcePlayer.teamId());
        target.setAgentId(
            sourcePlayer.agent() == null
                ? null
                : sourcePlayer.agent().id()
        );
        target.setAgentName(
            sourcePlayer.agent() == null
                || sourcePlayer.agent().name() == null
                ? "Unknown"
                : sourcePlayer.agent().name()
        );
        target.setResult(toResult(team));
        target.setKills(stats == null ? 0 : value(stats.kills()));
        target.setDeaths(stats == null ? 0 : value(stats.deaths()));
        target.setAssists(stats == null ? 0 : value(stats.assists()));
        target.setScore(stats == null ? 0 : value(stats.score()));
        target.setHeadshots(
            stats == null ? 0 : value(stats.headshots())
        );
        target.setBodyshots(
            stats == null ? 0 : value(stats.bodyshots())
        );
        target.setLegshots(
            stats == null ? 0 : value(stats.legshots())
        );
        target.setDamageDealt(
            stats == null || stats.damage() == null
                ? 0
                : value(stats.damage().dealt())
        );
        target.setRoundsPlayed(rounds);
        target.setAcs(
            roundBasedMode ? average(target.getScore(), rounds) : null
        );
        target.setAdr(
            roundBasedMode
                ? average(target.getDamageDealt(), rounds)
                : null
        );
        target.setCompetitiveTier(
            toCompetitiveTier(sourcePlayer.tier())
        );

        // Match history v4 does not reliably expose historical RR.
        target.setRankRating(null);
        target.setMvp(isMvp(source, sourcePlayer));
        return target;
    }

    private HenrikMatchMetadata requireMetadata(
        HenrikMatchData source
    ) {
        if (source == null || source.metadata() == null) {
            throw new IllegalArgumentException(
                "match metadata must not be null"
            );
        }
        return source.metadata();
    }

    private Integer toDurationSeconds(Long milliseconds) {
        if (milliseconds == null) {
            return null;
        }
        return Math.toIntExact(milliseconds / 1_000);
    }

    private GameMode toGameMode(
        HenrikMatchMetadata.HenrikQueue queue
    ) {
        if (queue == null) {
            return GameMode.OTHER;
        }

        String rawValue = queue.id() == null
            ? queue.name()
            : queue.id();

        if (rawValue == null) {
            return GameMode.OTHER;
        }

        String value = normalize(rawValue);

        return switch (value) {
            case "competitive" -> GameMode.COMPETITIVE;
            case "unrated" -> GameMode.UNRATED;
            case "swiftplay" -> GameMode.SWIFTPLAY;
            case "spikerush" -> GameMode.SPIKE_RUSH;
            case "deathmatch" -> GameMode.DEATHMATCH;
            case "teamdeathmatch", "hurm" ->
                GameMode.TEAM_DEATHMATCH;

            // Henrik/Riot naming may vary. In this project SKIRMISH is
            // intentionally considered equivalent to ESCALATION.
            case "escalation", "skirmish", "ggteam" ->
                GameMode.ESCALATION;

            case "premier" -> GameMode.PREMIER;
            case "custom" -> GameMode.CUSTOM;
            default -> GameMode.OTHER;
        };
    }

    private MatchResult toResult(HenrikMatchTeam team) {
        if (team == null || team.won() == null) {
            return MatchResult.UNKNOWN;
        }
        return team.won() ? MatchResult.WIN : MatchResult.LOSS;
    }

    private CompetitiveTier toCompetitiveTier(
        HenrikMatchPlayer.HenrikTier tier
    ) {
        if (tier == null || tier.name() == null
            || tier.name().isBlank()) {
            return CompetitiveTier.UNRANKED;
        }

        String normalized = tier.name()
            .trim()
            .toUpperCase(Locale.ROOT)
            .replace('-', '_')
            .replace(' ', '_');

        try {
            return CompetitiveTier.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return CompetitiveTier.UNRANKED;
        }
    }

    private Integer teamScore(
        HenrikMatchData source,
        String teamId
    ) {
        HenrikMatchTeam team = findTeam(source, teamId);
        return team == null || team.rounds() == null
            ? null
            : team.rounds().won();
    }

    private HenrikMatchTeam findTeam(
        HenrikMatchData source,
        String teamId
    ) {
        if (teamId == null) {
            return null;
        }

        return source.teams().stream()
            .filter(team -> teamId.equalsIgnoreCase(team.teamId()))
            .findFirst()
            .orElse(null);
    }

    private int roundsPlayed(HenrikMatchTeam team) {
        if (team == null || team.rounds() == null) {
            return 0;
        }

        return value(team.rounds().won())
            + value(team.rounds().lost());
    }

    private boolean supportsRoundAverages(GameMode gameMode) {
        return switch (gameMode) {
            case COMPETITIVE,
                 UNRATED,
                 SWIFTPLAY,
                 SPIKE_RUSH,
                 PREMIER,
                 CUSTOM -> true;
            case DEATHMATCH,
                 TEAM_DEATHMATCH,
                 ESCALATION,
                 OTHER -> false;
        };
    }

    private boolean isMvp(
        HenrikMatchData source,
        HenrikMatchPlayer player
    ) {
        if (player.stats() == null || player.stats().score() == null) {
            return false;
        }

        int playerScore = player.stats().score();
        int highestScore = source.players().stream()
            .map(HenrikMatchPlayer::stats)
            .filter(stats -> stats != null && stats.score() != null)
            .mapToInt(HenrikMatchPlayer.HenrikPlayerStats::score)
            .max()
            .orElse(Integer.MIN_VALUE);

        // Ties are intentionally accepted: every top-scoring player is MVP.
        return playerScore == highestScore;
    }

    private BigDecimal average(int total, int rounds) {
        if (rounds <= 0) {
            return null;
        }

        return BigDecimal.valueOf(total)
            .divide(
                BigDecimal.valueOf(rounds),
                2,
                RoundingMode.HALF_UP
            );
    }

    private String normalize(String value) {
        return value
            .toLowerCase(Locale.ROOT)
            .replace("_", "")
            .replace("-", "")
            .replace(" ", "");
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
