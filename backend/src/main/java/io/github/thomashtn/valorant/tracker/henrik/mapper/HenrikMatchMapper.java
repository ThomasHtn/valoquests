package io.github.thomashtn.valorant.tracker.henrik.mapper;

import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchHistoryResponse.HenrikMatchData;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchMetadata;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchPlayer;
import io.github.thomashtn.valorant.tracker.henrik.dto.match.HenrikMatchTeam;
import io.github.thomashtn.valorant.tracker.match.entity.PlayerMatch;
import io.github.thomashtn.valorant.tracker.match.entity.Season;
import io.github.thomashtn.valorant.tracker.match.entity.ValorantMatch;
import io.github.thomashtn.valorant.tracker.match.model.GameMode;
import io.github.thomashtn.valorant.tracker.match.model.GameModeSource;
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

    /**
     * Logger used to report unmapped Henrik payload values.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(HenrikMatchMapper.class);

    /**
     * Maps shared match metadata to the persisted match entity.
     *
     * @param source Henrik match payload
     * @param season season the match belongs to, already resolved
     * @return the match entity, not yet persisted
     */
    public ValorantMatch toValorantMatch(
        HenrikMatchData source,
        Season season
    ) {
        HenrikMatchMetadata metadata = requireMetadata(source);
        GameModeResolution resolution = toGameModeResolution(metadata);

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
        target.setGameMode(resolution.gameMode());
        target.setGameModeSource(resolution.source());
        target.setRedScore(teamScore(source, "Red"));
        target.setBlueScore(teamScore(source, "Blue"));
        return target;
    }

    /**
     * Maps the tracked player's statistics for one persisted match.
     *
     * @param source       Henrik match payload
     * @param sourcePlayer the tracked player's entry in that payload
     * @param player       tracked player the statistics belong to
     * @param match        persisted match the statistics attach to
     * @return the player-match association, not yet persisted
     */
    public PlayerMatch toPlayerMatch(
        HenrikMatchData source,
        HenrikMatchPlayer sourcePlayer,
        Player player,
        ValorantMatch match
    ) {
        HenrikMatchTeam team = findTeam(
            source,
            sourcePlayer.teamId()
        );

        PlayerMatch target = new PlayerMatch();
        target.setPlayer(player);
        target.setMatch(match);
        target.setTeamId(sourcePlayer.teamId());
        target.setResult(toResult(team));
        target.setRoundsPlayed(roundsPlayed(team));
        target.setCompetitiveTier(
            toCompetitiveTier(sourcePlayer.tier())
        );

        // Match history v4 does not reliably expose historical RR.
        target.setRankRating(null);
        target.setMvp(isMvp(source, sourcePlayer));

        applyAgent(target, sourcePlayer.agent());
        applyScoreboard(target, sourcePlayer.stats());
        applyRoundAverages(target, sourcePlayer.stats(), match.getGameMode());
        return target;
    }

    /**
     * Copies the agent a player used, falling back to a placeholder name.
     */
    private void applyAgent(
        PlayerMatch target,
        HenrikMatchPlayer.HenrikAgent agent
    ) {
        target.setAgentId(agent == null ? null : agent.id());
        target.setAgentName(
            agent == null || agent.name() == null
                ? "Unknown"
                : agent.name()
        );
    }

    /**
     * Copies the scoreboard counters reported for one player.
     *
     * <p>Henrik omits the whole block for some payloads. The counters then keep the zero every
     * column already defaults to, which is what an absent scoreboard means here.
     */
    private void applyScoreboard(
        PlayerMatch target,
        HenrikMatchPlayer.HenrikPlayerStats stats
    ) {
        if (stats == null) {
            return;
        }

        target.setKills(value(stats.kills()));
        target.setDeaths(value(stats.deaths()));
        target.setAssists(value(stats.assists()));
        target.setScore(value(stats.score()));
        target.setHeadshots(value(stats.headshots()));
        target.setBodyshots(value(stats.bodyshots()));
        target.setLegshots(value(stats.legshots()));
        target.setDamageDealt(
            stats.damage() == null ? 0 : value(stats.damage().dealt())
        );
    }

    /**
     * Derives the per-round averages, which only mean something in a round-based mode.
     *
     * <p>Must run after the scoreboard and round count are set, since it averages them.
     */
    private void applyRoundAverages(
        PlayerMatch target,
        HenrikMatchPlayer.HenrikPlayerStats stats,
        GameMode gameMode
    ) {
        if (!gameMode.isRoundBased()) {
            return;
        }

        int rounds = target.getRoundsPlayed();
        target.setAcs(average(target.getScore(), rounds));

        // Henrik omits the damage breakdown for some modes, Skirmish among them. The persisted
        // total then falls back to zero because the column is not nullable, so ADR must stay unset
        // rather than report a zero average that would drag the player's statistics down.
        boolean damageReported = stats != null
            && stats.damage() != null
            && stats.damage().dealt() != null;
        target.setAdr(
            damageReported ? average(target.getDamageDealt(), rounds) : null
        );
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
     * Resolves the game mode from the identifiers Henrik exposes, most specific first, along with the
     * source that classifies how confidently it was resolved.
     *
     * <p>The queue slug is authoritative and yields {@link GameModeSource#PROVIDED}. The display name
     * and mode type are fallbacks covering matches where Henrik returns a blank slug, as it does for
     * some custom games, or renames a queue; both yield {@link GameModeSource#INFERRED}. The mode type
     * is also ambiguous for bomb-based queues, which all report {@code Standard}, so it is tried last.
     *
     * <p>Neither fallback is reliable on its own: Henrik returns no display name for the Skirmish
     * queue, and it returns the map name rather than a mode label for the new-map queue. Only their
     * combination classifies every queue observed so far.
     *
     * <p>Deliberately silent, so the import layer can classify a match to decide whether to store it,
     * and enrichment can compare sources, without emitting a diagnostic for every page. The warning
     * belongs to {@link #toGameModeResolution}, which runs once per persisted match.
     *
     * @param metadata Henrik match metadata
     * @return the resolved mode and the source that classified it
     */
    public GameModeResolution resolveGameModeWithSource(HenrikMatchMetadata metadata) {
        HenrikMatchMetadata.HenrikQueue queue = metadata.queue();

        if (queue == null) {
            return new GameModeResolution(GameMode.OTHER, GameModeSource.UNKNOWN);
        }

        return GameMode.fromIdentifier(queue.id())
            .map(gameMode -> new GameModeResolution(gameMode, GameModeSource.PROVIDED))
            .or(() -> GameMode.fromIdentifier(queue.name())
                .map(gameMode -> new GameModeResolution(gameMode, GameModeSource.INFERRED)))
            .or(() -> GameMode.fromIdentifier(queue.modeType())
                .map(gameMode -> new GameModeResolution(gameMode, GameModeSource.INFERRED)))
            .orElseGet(() -> new GameModeResolution(GameMode.OTHER, GameModeSource.UNKNOWN));
    }

    /**
     * Resolves the game mode, ignoring its source.
     *
     * @param metadata Henrik match metadata
     * @return the resolved mode, or {@link GameMode#OTHER} when no identifier matches
     * @see #resolveGameModeWithSource(HenrikMatchMetadata)
     */
    public GameMode resolveGameMode(HenrikMatchMetadata metadata) {
        return resolveGameModeWithSource(metadata).gameMode();
    }

    /**
     * Resolves the game mode and reports the queues this application cannot classify.
     *
     * <p>An unresolved queue is logged rather than silently bucketed into {@link GameMode#OTHER}, so
     * a newly released Valorant mode surfaces in the synchronization logs. Called only from
     * {@link #toValorantMatch}, which runs on the creation path alone: the warning is therefore
     * emitted exactly once per persisted match, never per page and never for a match already stored.
     */
    private GameModeResolution toGameModeResolution(
        HenrikMatchMetadata metadata
    ) {
        HenrikMatchMetadata.HenrikQueue queue = metadata.queue();

        if (queue == null) {
            LOGGER.warn(
                "Match {} has no queue: game mode set to OTHER",
                metadata.matchId()
            );
            return new GameModeResolution(GameMode.OTHER, GameModeSource.UNKNOWN);
        }

        GameModeResolution resolution = resolveGameModeWithSource(metadata);
        if (resolution.gameMode() == GameMode.OTHER) {
            LOGGER.warn(
                "Unresolved game mode for match {}: queue id={}, "
                    + "name={}, modeType={}. Falling back to OTHER.",
                metadata.matchId(),
                queue.id(),
                queue.name(),
                queue.modeType()
            );
        }
        return resolution;
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

    /**
     * A resolved game mode paired with how confidently it was determined.
     *
     * @param gameMode resolved mode
     * @param source   identifier tier that resolved it
     */
    public record GameModeResolution(GameMode gameMode, GameModeSource source) {
    }
}
