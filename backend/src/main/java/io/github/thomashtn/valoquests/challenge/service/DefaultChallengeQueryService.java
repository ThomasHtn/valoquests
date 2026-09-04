package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.dto.CurrentChallengesResponse;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides the collective progress exposed by the current challenges endpoint.
 *
 * <p>Read-only: the weekly pack and the day's challenge are drawn by the recalculation that
 * follows every synchronization and by the daily tick, never by a read. A day whose challenge is
 * not drawn yet is simply absent from the response.
 */
@Service
@Transactional(readOnly = true)
public class DefaultChallengeQueryService implements ChallengeQueryService {

    /**
     * Number of days in a week, to derive the Sunday from the Monday.
     */
    private static final int DAYS_TO_SUNDAY = 6;

    /**
     * Orders a weekly pack from the easiest to the hardest tier.
     */
    private static final Comparator<WeeklyChallenge> EASIEST_FIRST =
        Comparator.comparingInt(selection -> selection.getChallenge().getDifficulty().ordinal());

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
     * Parser used to expose the resolved definition of each selection.
     */
    private final ChallengeDefinitionParser definitionParser;

    /**
     * Barème saying what a challenge of each weight is worth.
     */
    private final ScoringRuleset ruleset;

    /**
     * Source of the reference and week index the rewards are priced with.
     */
    private final ChallengeCalibrationSource calibrationSource;

    /**
     * Calendar resolving the current week and day.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the current-challenge query service.
     *
     * @param weeklyChallengeRepository weekly challenge repository
     * @param progressRepository        player progress repository
     * @param playerRepository          tracked-player repository
     * @param definitionParser          challenge-definition parser
     * @param ruleset                   scoring ruleset
     * @param calibrationSource         calibration source
     * @param weekCalendar              calendar resolving the current week
     */
    public DefaultChallengeQueryService(
        WeeklyChallengeRepository weeklyChallengeRepository,
        PlayerChallengeProgressRepository progressRepository,
        PlayerRepository playerRepository,
        ChallengeDefinitionParser definitionParser,
        ScoringRuleset ruleset,
        ChallengeCalibrationSource calibrationSource,
        WeekCalendar weekCalendar
    ) {
        this.weeklyChallengeRepository = weeklyChallengeRepository;
        this.progressRepository = progressRepository;
        this.playerRepository = playerRepository;
        this.definitionParser = definitionParser;
        this.ruleset = ruleset;
        this.calibrationSource = calibrationSource;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Returns collective progress for every challenge of the current week, daily draws included.
     *
     * @return current-week challenge response
     */
    @Override
    public CurrentChallengesResponse findCurrent() {
        LocalDate weekStart = weekCalendar.currentWeekStart();
        ChallengeCalibration calibration = calibrationSource.forWeek(weekStart);
        Map<Long, List<PlayerChallengeProgress>> progressByChallenge =
            groupProgressByChallenge(weekStart);
        int totalPlayers = Math.toIntExact(
            playerRepository.countByStatus(Player.COMPETITIVE_STATUS)
        );

        List<WeeklyChallenge> selections =
            weeklyChallengeRepository.findAllByWeekStartAndFinalizedAtIsNullOrderByIdAsc(weekStart);

        List<CurrentChallengesResponse.ChallengeProgressResponse> weekly = selections.stream()
            .filter(selection -> selection.getCadence() == ChallengeCadence.WEEKLY)
            .sorted(EASIEST_FIRST)
            .map(selection -> toResponse(selection, calibration, progressByChallenge, totalPlayers))
            .toList();

        List<CurrentChallengesResponse.ChallengeProgressResponse> dailies = selections.stream()
            .filter(selection -> selection.getCadence() == ChallengeCadence.DAILY)
            .sorted(Comparator.comparing(WeeklyChallenge::getDay))
            .map(selection -> toResponse(selection, calibration, progressByChallenge, totalPlayers))
            .toList();

        return new CurrentChallengesResponse(
            weekStart,
            weekStart.plusDays(DAYS_TO_SUNDAY),
            weekCalendar.today(),
            findLastSuccessfulSynchronizationAt(),
            weekly,
            dailies
        );
    }

    /**
     * Groups persisted progress rows by selection identifier.
     *
     * @param weekStart Monday identifying the requested week
     * @return progress rows indexed by selection identifier
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
     * Converts a selection and its progress into an API response.
     *
     * @param selection           selection to convert
     * @param calibration         calibration in force for the week
     * @param progressByChallenge progress rows indexed by selection identifier
     * @param totalPlayers        number of active players
     * @return challenge response
     */
    private CurrentChallengesResponse.ChallengeProgressResponse toResponse(
        WeeklyChallenge selection,
        ChallengeCalibration calibration,
        Map<Long, List<PlayerChallengeProgress>> progressByChallenge,
        int totalPlayers
    ) {
        Challenge challenge = selection.getChallenge();
        ChallengeDefinition definition = definitionParser.parse(selection);
        int completedPlayers = countCompletedPlayers(
            progressByChallenge.getOrDefault(selection.getId(), List.of())
        );
        double weight = ruleset.challengeWeight(selection.getCadence(), challenge.getDifficulty());

        return new CurrentChallengesResponse.ChallengeProgressResponse(
            selection.getId(),
            challenge.getCode(),
            challenge.getName(),
            challenge.getDescription(),
            selection.getCadence(),
            challenge.getDifficulty(),
            selection.getDay(),
            definition.isCompetitiveOnly(),
            ChallengeMetricLabels.of(definition),
            definition.progressTarget(),
            ruleset.challengeSurvivors(calibration.reference(), weight, calibration.weekIndex()),
            ruleset.challengeRankingPoints(calibration.reference(), weight),
            completedPlayers,
            totalPlayers,
            calculateCompletionPercentage(completedPlayers, totalPlayers)
        );
    }

    /**
     * Counts completed progress rows belonging to an active player.
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
