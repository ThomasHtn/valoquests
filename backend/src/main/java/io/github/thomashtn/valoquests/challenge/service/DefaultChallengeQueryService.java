package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.dto.CurrentChallengesResponse;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeSelectionRepository;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.scoring.model.CampaignDifficulty;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCalibration;
import io.github.thomashtn.valoquests.scoring.service.ScoringRuleset;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides the progress exposed by the current challenges endpoint, collective and per player.
 *
 * <p>Read-only: the weekly pack and the day's challenge are drawn by the recalculation that
 * follows every synchronization and by the daily tick, never by a read. A day whose challenge is
 * not drawn yet is simply absent from the response.
 */
@Service
@Transactional(readOnly = true)
public class DefaultChallengeQueryService implements ChallengeQueryService {

    /**
     * Repository used to retrieve the challenges selected for a week.
     */
    private final ChallengeSelectionRepository challengeSelectionRepository;

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
     * Scoring table saying what a challenge of each weight is worth.
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
     * @param challengeSelectionRepository challenge selection repository
     * @param progressRepository        player progress repository
     * @param playerRepository          tracked-player repository
     * @param definitionParser          challenge-definition parser
     * @param ruleset                   scoring ruleset
     * @param calibrationSource         calibration source
     * @param weekCalendar              calendar resolving the current week
     */
    public DefaultChallengeQueryService(
        ChallengeSelectionRepository challengeSelectionRepository,
        PlayerChallengeProgressRepository progressRepository,
        PlayerRepository playerRepository,
        ChallengeDefinitionParser definitionParser,
        ScoringRuleset ruleset,
        ChallengeCalibrationSource calibrationSource,
        WeekCalendar weekCalendar
    ) {
        this.challengeSelectionRepository = challengeSelectionRepository;
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
        List<CurrentChallengesResponse.RosterPlayerResponse> roster = playerRepository
            .findAllByStatusOrderByIdAsc(Player.COMPETITIVE_STATUS)
            .stream()
            .map(player -> new CurrentChallengesResponse.RosterPlayerResponse(
                player.getId(),
                player.getDisplayName(),
                player.getPortrait()
            ))
            .toList();
        List<Long> rosterIds = roster.stream()
            .map(CurrentChallengesResponse.RosterPlayerResponse::id)
            .toList();

        List<ChallengeSelection> selections =
            challengeSelectionRepository.findAllByWeekStartAndFinalizedAtIsNullOrderByIdAsc(weekStart);

        List<CurrentChallengesResponse.ChallengeProgressResponse> weekly = selections.stream()
            .filter(selection -> selection.getCadence() == ChallengeCadence.WEEKLY)
            .sorted(ChallengeSelection.EASIEST_FIRST)
            .map(selection -> toResponse(selection, calibration, progressByChallenge, rosterIds))
            .toList();

        List<CurrentChallengesResponse.ChallengeProgressResponse> dailies = selections.stream()
            .filter(selection -> selection.getCadence() == ChallengeCadence.DAILY)
            .sorted(Comparator.comparing(ChallengeSelection::getDay))
            .map(selection -> toResponse(selection, calibration, progressByChallenge, rosterIds))
            .toList();

        return new CurrentChallengesResponse(
            weekStart,
            WeekCalendar.lastDayOf(weekStart),
            weekCalendar.today(),
            findLastSuccessfulSynchronizationAt(),
            roster,
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
            .findAllBySelectionWeekStartOrderByPlayerIdAscSelectionIdAsc(
                weekStart
            )
            .stream()
            .collect(Collectors.groupingBy(
                progress -> progress.getSelection().getId()
            ));
    }

    /**
     * Converts a selection and its progress into an API response.
     *
     * @param selection           selection to convert
     * @param calibration         calibration in force for the week
     * @param progressByChallenge progress rows indexed by selection identifier
     * @param rosterIds           active players, in roster order
     * @return challenge response
     */
    private CurrentChallengesResponse.ChallengeProgressResponse toResponse(
        ChallengeSelection selection,
        ChallengeCalibration calibration,
        Map<Long, List<PlayerChallengeProgress>> progressByChallenge,
        List<Long> rosterIds
    ) {
        Challenge challenge = selection.getChallenge();
        ChallengeDefinition definition = definitionParser.parse(selection);
        String description = ChallengeDescriptionResolver.resolve(
            challenge.getDescription(),
            definitionParser.parse(challenge, CampaignDifficulty.AMATEUR),
            definition
        );
        List<PlayerChallengeProgress> rows = progressByChallenge.getOrDefault(selection.getId(), List.of());
        List<Long> completedPlayerIds = completedPlayerIds(rows);
        int reward = ruleset.challengeReward(selection.getCadence(), challenge.getTier(), calibration);

        return new CurrentChallengesResponse.ChallengeProgressResponse(
            selection.getId(),
            challenge.getCode(),
            challenge.getName(),
            description,
            selection.getCadence(),
            challenge.getTier(),
            selection.getDay(),
            ChallengeMetricLabels.of(definition),
            definition.progressTarget(),
            reward,
            completedPlayerIds,
            playerProgress(rows, rosterIds)
        );
    }

    /**
     * Lays out every active player's progress on one selection, in roster order.
     *
     * <p>A player without a row has not been evaluated on it yet: they stand at zero on it rather
     * than being absent, so a reader never has to guess who is missing.
     *
     * @param progressRows progress rows of the selection
     * @param rosterIds    active players, in roster order
     * @return one line per active player
     */
    private List<CurrentChallengesResponse.PlayerProgressResponse> playerProgress(
        List<PlayerChallengeProgress> progressRows,
        List<Long> rosterIds
    ) {
        Map<Long, PlayerChallengeProgress> byPlayer = progressRows.stream()
            .collect(Collectors.toMap(
                progress -> progress.getPlayer().getId(),
                progress -> progress,
                (first, second) -> first
            ));
        return rosterIds.stream()
            .map(playerId -> {
                PlayerChallengeProgress progress = byPlayer.get(playerId);
                return new CurrentChallengesResponse.PlayerProgressResponse(
                    playerId,
                    progress == null ? BigDecimal.ZERO : progress.getCurrentValue(),
                    progress != null && progress.isCompleted()
                );
            })
            .toList();
    }

    /**
     * Lists the active players whose progress row is completed.
     *
     * <p>An inactive player can still complete a challenge, but it must never inflate the
     * collective completion reported here, which is read against the active roster.
     *
     * @param progressRows progress rows to inspect
     * @return identifiers of the active players who completed the challenge, ascending
     */
    private List<Long> completedPlayerIds(List<PlayerChallengeProgress> progressRows) {
        return progressRows.stream()
            .filter(PlayerChallengeProgress::isCompleted)
            .filter(progress -> progress.getPlayer().isCompetitive())
            .map(progress -> progress.getPlayer().getId())
            .sorted()
            .toList();
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
