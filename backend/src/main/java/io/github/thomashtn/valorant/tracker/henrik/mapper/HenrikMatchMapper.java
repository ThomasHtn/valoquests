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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Maps Henrik match transport objects to persistence entities.
 */
@Component
public class HenrikMatchMapper {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(HenrikMatchMapper.class);

    /**
     * Maps shared match metadata to the persisted match entity.
     */
    public ValorantMatch toValorantMatch(
        HenrikMatchData source,
        Season season
    ) {
        HenrikMatchMetadata metadata = requireMetadata(source);
        GameMode gameMode = toGameMode(metadata);

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

    /**
     * Maps the tracked player's statistics for one persisted match.
     */
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
        boolean roundBasedMode = match.getGameMode().isRoundBased();

        // Henrik omits the damage breakdown for some modes, Skirmish among them. The persisted
        // total then falls back to zero because the column is not nullable, so ADR must stay unset
        // rather than report a zero average that would drag the player's statistics down.
        boolean damageReported = stats != null
            && stats.damage() != null
            && stats.damage().dealt() != null;

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
            roundBasedMode && damageReported
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

    /**
     * Resolves the game mode from the identifiers Henrik exposes, most specific first.
     *
     * <p>The queue slug is authoritative. The display name covers matches where Henrik returns a
     * blank slug, as it does for some custom games. The mode type comes last and is only a safety
     * net for a queue renamed by Riot: it names the <em>ruleset</em>, not the queue, so a custom
     * game played with the Skirmish ruleset reports {@code Skirmish} while belonging to the custom
     * queue. It is also ambiguous for bomb-based queues, which all report {@code Standard}.
     *
     * <p>Neither of the first two levels is reliable on its own: Henrik returns no display name for
     * the Skirmish queue, and it returns the map name rather than a mode label for the new-map
     * queue. Only their combination classifies every queue observed so far.
     *
     * <p>Deliberately silent, so the import layer can classify a match to decide whether to store it
     * without emitting a diagnostic for every page. The warning belongs to {@link #toGameMode},
     * which runs once per persisted match.
     *
     * @param metadata Henrik match metadata
     * @return the resolved mode, or {@link GameMode#OTHER} when no identifier matches
     */
    public GameMode resolveGameMode(HenrikMatchMetadata metadata) {
        HenrikMatchMetadata.HenrikQueue queue = metadata.queue();

        if (queue == null) {
            return GameMode.OTHER;
        }

        return GameMode.fromIdentifier(queue.id())
            .or(() -> GameMode.fromIdentifier(queue.name()))
            .or(() -> GameMode.fromIdentifier(queue.modeType()))
            .orElse(GameMode.OTHER);
    }

    /**
     * Resolves the game mode and reports the queues this application cannot classify.
     *
     * <p>An unresolved queue is logged rather than silently bucketed into {@link GameMode#OTHER}, so
     * a newly released Valorant mode surfaces in the synchronization logs. Called only from
     * {@link #toValorantMatch}, which runs on the creation path alone: the warning is therefore
     * emitted exactly once per persisted match, never per page and never for a match already stored.
     */
    private GameMode toGameMode(
        HenrikMatchMetadata metadata
    ) {
        HenrikMatchMetadata.HenrikQueue queue = metadata.queue();

        if (queue == null) {
            LOGGER.warn(
                "Match {} has no queue: game mode set to OTHER",
                metadata.matchId()
            );
            return GameMode.OTHER;
        }

        GameMode gameMode = resolveGameMode(metadata);
        if (gameMode == GameMode.OTHER) {
            LOGGER.warn(
                "Unresolved game mode for match {}: queue id={}, "
                    + "name={}, modeType={}. Falling back to OTHER.",
                metadata.matchId(),
                queue.id(),
                queue.name(),
                queue.modeType()
            );
        }
        return gameMode;
    }

    private MatchResult toResult(HenrikMatchTeam team) {
        if (team == null || team.won() == null) {
            return MatchResult.UNKNOWN;
        }
        return Boolean.TRUE.equals(team.won())
            ? MatchResult.WIN
            : MatchResult.LOSS;
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
        } catch (IllegalArgumentException _) {
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

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
