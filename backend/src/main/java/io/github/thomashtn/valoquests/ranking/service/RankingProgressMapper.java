package io.github.thomashtn.valoquests.ranking.service;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.ranking.dto.CurrentRankingResponse.ChallengeProgressResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lays out, for every player of a week, where they stand on each challenge the board shows.
 *
 * <p>The board shows the week's pack and the day's daily challenge, one line per challenge for every
 * player, whether a progress row exists yet or not: a player who has not been evaluated on a
 * challenge is at zero on it, not absent from it, and a grid with holes is a grid the reader has to
 * reconstruct. Yesterday's daily is left out: it is decided, and its points are already in the row.
 */
@Service
@Transactional(readOnly = true)
public class RankingProgressMapper {

    /**
     * Repository holding the week's selections.
     */
    private final WeeklyChallengeRepository weeklyChallengeRepository;

    /**
     * Repository holding the week's progress rows.
     */
    private final PlayerChallengeProgressRepository progressRepository;

    /**
     * Parser resolving a selection's stored conditions.
     */
    private final ChallengeDefinitionParser definitionParser;

    /**
     * Reader pricing one selection in ranking points.
     */
    private final ChallengePointsReader challengePointsReader;

    /**
     * Creates the ranking progress mapper.
     *
     * @param weeklyChallengeRepository weekly challenge repository
     * @param progressRepository        player challenge progress repository
     * @param definitionParser          challenge definition parser
     * @param challengePointsReader     challenge points reader
     */
    public RankingProgressMapper(
        WeeklyChallengeRepository weeklyChallengeRepository,
        PlayerChallengeProgressRepository progressRepository,
        ChallengeDefinitionParser definitionParser,
        ChallengePointsReader challengePointsReader
    ) {
        this.weeklyChallengeRepository = weeklyChallengeRepository;
        this.progressRepository = progressRepository;
        this.definitionParser = definitionParser;
        this.challengePointsReader = challengePointsReader;
    }

    /**
     * Lays out one week's board.
     *
     * @param weekStart Monday identifying the week
     * @param today     day whose daily challenge is shown next to the pack
     * @param playerIds players the board has a line for
     * @return each player's progress, weekly challenges first then the day's, indexed by player
     */
    public WeekBoard forWeek(LocalDate weekStart, LocalDate today, Collection<Long> playerIds) {
        int reference = challengePointsReader.referenceFor(weekStart);
        int weekIndex = challengePointsReader.weekIndexFor(weekStart);

        List<WeeklyChallenge> selections = weeklyChallengeRepository
            .findAllByWeekStartOrderByIdAsc(weekStart)
            .stream()
            .filter(selection -> selection.getCadence() == ChallengeCadence.WEEKLY
                || today.equals(selection.getDay()))
            .toList();

        Map<Long, Map<Long, PlayerChallengeProgress>> progressByPlayer = progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(weekStart)
            .stream()
            .collect(Collectors.groupingBy(
                progress -> progress.getPlayer().getId(),
                Collectors.toMap(progress -> progress.getWeeklyChallenge().getId(), progress -> progress)
            ));

        Map<Long, List<ChallengeProgressResponse>> board = new HashMap<>(playerIds.size());
        for (Long playerId : playerIds) {
            Map<Long, PlayerChallengeProgress> rows = progressByPlayer.getOrDefault(playerId, Map.of());
            List<ChallengeProgressResponse> lines = new ArrayList<>(selections.size());

            for (WeeklyChallenge selection : selections) {
                lines.add(toLine(selection, rows.get(selection.getId()), reference, weekIndex));
            }

            board.put(playerId, List.copyOf(lines));
        }

        int weeklyCount = (int) selections.stream()
            .filter(selection -> selection.getCadence() == ChallengeCadence.WEEKLY)
            .count();

        return new WeekBoard(weeklyCount, board);
    }

    /**
     * Maps one player's standing on one selection.
     *
     * @param selection selected challenge
     * @param progress  the player's stored progress, or {@code null} when not evaluated yet
     * @param reference reference in force for the week
     * @param weekIndex campaign week of that week, {@code 0} between two campaigns
     * @return the line
     */
    private ChallengeProgressResponse toLine(
        WeeklyChallenge selection,
        PlayerChallengeProgress progress,
        int reference,
        int weekIndex
    ) {
        ChallengeDefinition definition = definitionParser.parse(selection);
        Challenge challenge = selection.getChallenge();
        String metric = definition.conditions().stream()
            .map(condition -> condition.metric().name())
            .distinct()
            .collect(Collectors.joining(" + "));
        String unit = definition.conditions().size() == 1
            ? unitOf(definition.singleCondition().metric())
            : null;

        return new ChallengeProgressResponse(
            selection.getId(),
            challenge.getCode(),
            challenge.getName(),
            challenge.getCadence(),
            challenge.getDifficulty(),
            selection.getDay(),
            metric,
            progress == null ? BigDecimal.ZERO : progress.getCurrentValue(),
            progress == null ? definition.progressTarget() : progress.getTargetValue(),
            unit,
            progress != null && progress.isCompleted(),
            challengePointsReader.pointsOf(selection, reference, weekIndex)
        );
    }

    /**
     * Returns the display unit associated with one challenge metric.
     *
     * @param metric measured metric
     * @return its unit
     */
    private String unitOf(ChallengeMetric metric) {
        return switch (metric) {
            case DAMAGE_DEALT, SCORE -> "points";
            case KD, HEADSHOT_RATE -> "ratio";
            case ACS, ADR -> "per round";
            case PLAY_DAY -> "days";
            case MATCHES_PLAYED, MATCHES_WON -> "matches";
            case KILLS -> "kills";
            case ASSISTS -> "assists";
            case HEADSHOTS -> "headshots";
            case ROUNDS_PLAYED -> "rounds";
        };
    }

    /**
     * One week's board.
     *
     * @param weeklyChallengeCount weekly challenges selected for the week
     * @param progressByPlayer     each player's lines, indexed by player identifier
     */
    public record WeekBoard(int weeklyChallengeCount, Map<Long, List<ChallengeProgressResponse>> progressByPlayer) {

        /**
         * Creates an immutable board.
         */
        public WeekBoard {
            progressByPlayer = Map.copyOf(progressByPlayer);
        }

        /**
         * Returns one player's lines.
         *
         * @param playerId internal player identifier
         * @return the lines, empty when the player has none
         */
        public List<ChallengeProgressResponse> of(long playerId) {
            return progressByPlayer.getOrDefault(playerId, List.of());
        }
    }
}
