package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.dto.CurrentChallengesResponse;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides the collective progress exposed by the current challenges endpoint.
 */
@Service
@Transactional(readOnly = true)
public class DefaultChallengeQueryService implements ChallengeQueryService {

    /**
     * Repository used to retrieve the challenges selected for a week.
     */
    private final WeeklyChallengeRepository weeklyChallengeRepository;

    /**
     * Repository used to retrieve persisted player progress.
     */
    private final PlayerChallengeProgressRepository progressRepository;

    /**
     * Repository used to count players and resolve the latest synchronization.
     */
    private final PlayerRepository playerRepository;

    /**
     * Parser used to expose typed challenge-definition values.
     */
    private final ChallengeDefinitionParser definitionParser;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the current-challenge query service.
     *
     * @param weeklyChallengeRepository weekly challenge repository
     * @param progressRepository        player progress repository
     * @param playerRepository          tracked-player repository
     * @param definitionParser          challenge-definition parser
     * @param weekCalendar       calendar resolving the current week
     */
    public DefaultChallengeQueryService(
        WeeklyChallengeRepository weeklyChallengeRepository,
        PlayerChallengeProgressRepository progressRepository,
        PlayerRepository playerRepository,
        ChallengeDefinitionParser definitionParser,
        WeekCalendar weekCalendar
    ) {
        this.weeklyChallengeRepository = weeklyChallengeRepository;
        this.progressRepository = progressRepository;
        this.playerRepository = playerRepository;
        this.definitionParser = definitionParser;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Returns collective progress for every challenge of the current week.
     *
     * @return current-week challenge response
     */
    @Override
    public CurrentChallengesResponse findCurrent() {
        LocalDate weekStart = weekCalendar.currentWeekStart();
        List<WeeklyChallenge> weeklyChallenges = findWeeklyChallenges(weekStart);
        Map<Long, List<PlayerChallengeProgress>> progressByChallenge =
            groupProgressByChallenge(weekStart);
        int totalPlayers = Math.toIntExact(
            playerRepository.countByStatus(PlayerStatus.ACTIVE)
        );

        List<CurrentChallengesResponse.ChallengeProgressResponse> challenges =
            weeklyChallenges.stream()
                .map(weeklyChallenge -> toChallengeResponse(
                    weeklyChallenge,
                    progressByChallenge,
                    totalPlayers
                ))
                .toList();

        return new CurrentChallengesResponse(
            weekStart,
            weekStart.plusDays(6),
            findLastSuccessfulSynchronizationAt(),
            challenges
        );
    }

    /**
     * Retrieves active challenges for the requested week.
     *
     * @param weekStart Monday identifying the requested week
     * @return ordered weekly challenges
     */
    private List<WeeklyChallenge> findWeeklyChallenges(LocalDate weekStart) {
        return weeklyChallengeRepository
            .findAllByWeekStartAndFinalizedAtIsNullOrderByIdAsc(weekStart);
    }

    /**
     * Groups persisted progress rows by weekly challenge identifier.
     *
     * @param weekStart Monday identifying the requested week
     * @return progress rows indexed by weekly challenge identifier
     */
    private Map<Long, List<PlayerChallengeProgress>> groupProgressByChallenge(
        LocalDate weekStart
    ) {
        return progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(
                weekStart
            )
            .stream()
            .collect(Collectors.groupingBy(
                progress -> progress.getWeeklyChallenge().getId()
            ));
    }

    /**
     * Converts a weekly challenge and its progress into an API response.
     *
     * @param weeklyChallenge    weekly challenge to convert
     * @param progressByChallenge progress rows indexed by challenge identifier
     * @param totalPlayers       number of active players
     * @return challenge response
     */
    private CurrentChallengesResponse.ChallengeProgressResponse toChallengeResponse(
        WeeklyChallenge weeklyChallenge,
        Map<Long, List<PlayerChallengeProgress>> progressByChallenge,
        int totalPlayers
    ) {
        ChallengeDefinition definition = definitionParser.parse(
            weeklyChallenge.getChallenge()
        );
        List<PlayerChallengeProgress> progressRows = progressByChallenge
            .getOrDefault(weeklyChallenge.getId(), List.of());
        int completedPlayers = countCompletedPlayers(progressRows);

        return new CurrentChallengesResponse.ChallengeProgressResponse(
            weeklyChallenge.getId(),
            weeklyChallenge.getChallenge().getName(),
            weeklyChallenge.getChallenge().getDescription(),
            weeklyChallenge.getChallenge().getDifficulty(),
            resolveMetricLabel(definition),
            resolveTargetValue(definition, progressRows),
            weeklyChallenge.getChallenge().getDamage(),
            completedPlayers,
            totalPlayers,
            calculateCompletionPercentage(completedPlayers, totalPlayers)
        );
    }

    /**
     * Counts completed progress rows belonging to a competitive (active) player.
     *
     * <p>An inactive player can still complete a challenge, but it must never inflate the
     * collective completion reported here: {@code totalPlayers} only counts active players, so
     * the numerator must stay consistent with it.
     *
     * @param progressRows progress rows to inspect
     * @return number of completed rows from active players
     */
    private int countCompletedPlayers(List<PlayerChallengeProgress> progressRows) {
        return Math.toIntExact(
            progressRows.stream()
                .filter(PlayerChallengeProgress::isCompleted)
                .filter(progress -> progress.getPlayer().isCompetitive())
                .count()
        );
    }

    /**
     * Builds the metric label exposed by the endpoint.
     *
     * @param definition parsed challenge definition
     * @return distinct metric names joined in definition order
     */
    private String resolveMetricLabel(ChallengeDefinition definition) {
        return definition.conditions().stream()
            .map(condition -> condition.metric().name())
            .distinct()
            .collect(Collectors.joining(" + "));
    }

    /**
     * Resolves the target displayed for a simple or composite challenge.
     *
     * @param definition   parsed challenge definition
     * @param progressRows persisted progress rows
     * @return target value, or {@code null} when no composite target is stored
     */
    private BigDecimal resolveTargetValue(
        ChallengeDefinition definition,
        List<PlayerChallengeProgress> progressRows
    ) {
        if (definition.conditions().size() == 1) {
            return definition.singleCondition().target();
        }

        return progressRows.stream()
            .map(PlayerChallengeProgress::getTargetValue)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /**
     * Calculates collective challenge completion as a percentage.
     *
     * @param completedPlayers number of players who completed the challenge
     * @param totalPlayers     number of active players
     * @return completion percentage rounded to two decimal places
     */
    private BigDecimal calculateCompletionPercentage(
        int completedPlayers,
        int totalPlayers
    ) {
        if (totalPlayers == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(completedPlayers)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(totalPlayers), 2, RoundingMode.HALF_UP);
    }

    /**
     * Resolves the most recent successful synchronization timestamp.
     *
     * @return latest timestamp, or {@code null} when no player was synchronized
     */
    private Instant findLastSuccessfulSynchronizationAt() {
        return playerRepository
            .findLatestSuccessfulSynchronizationAt()
            .orElse(null);
    }
}
