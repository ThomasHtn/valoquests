package io.github.thomashtn.valorant.tracker.ranking.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valorant.tracker.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valorant.tracker.challenge.model.ChallengeMetric;
import io.github.thomashtn.valorant.tracker.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valorant.tracker.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valorant.tracker.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valorant.tracker.ranking.dto.CurrentRankingResponse;
import io.github.thomashtn.valorant.tracker.ranking.dto.RankingHistoryWeekResponse;
import io.github.thomashtn.valorant.tracker.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valorant.tracker.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valorant.tracker.shared.dto.PageResponse;
import io.github.thomashtn.valorant.tracker.shared.exception.InvalidRequestException;
import io.github.thomashtn.valorant.tracker.week.WeekCalendar;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides optimized read-only access to current and historical rankings.
 */
@Service
@Transactional(readOnly = true)
public class DefaultRankingQueryService implements RankingQueryService {

    /**
     * Maximum number of historical weeks accepted by one request.
     */
    private static final int MAXIMUM_PAGE_SIZE = 100;

    /**
     * Repository used to read weekly ranking snapshots.
     */
    private final WeeklyPlayerScoreRepository scoreRepository;

    /**
     * Repository used to read per-player challenge progress.
     */
    private final PlayerChallengeProgressRepository progressRepository;

    /**
     * Repository used to read challenges assigned to each week.
     */
    private final WeeklyChallengeRepository weeklyChallengeRepository;

    /**
     * Parser used to resolve challenge targets and display units.
     */
    private final ChallengeDefinitionParser definitionParser;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the ranking query service.
     *
     * @param scoreRepository           repository holding weekly player scores
     * @param progressRepository        repository holding per-player challenge progress
     * @param weeklyChallengeRepository repository holding the challenges selected for a week
     * @param definitionParser          parser turning stored challenge rules into definitions
     * @param weekCalendar              calendar resolving the current week
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public DefaultRankingQueryService(
        WeeklyPlayerScoreRepository scoreRepository,
        PlayerChallengeProgressRepository progressRepository,
        WeeklyChallengeRepository weeklyChallengeRepository,
        ChallengeDefinitionParser definitionParser,
        WeekCalendar weekCalendar
    ) {
        this.scoreRepository = scoreRepository;
        this.progressRepository = progressRepository;
        this.weeklyChallengeRepository = weeklyChallengeRepository;
        this.definitionParser = definitionParser;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Returns the current ranking and exact progress for every ranked player.
     */
    @Override
    public CurrentRankingResponse findCurrent() {
        LocalDate weekStart = weekCalendar.currentWeekStart();
        List<WeeklyPlayerScore> scores = scoreRepository
            .findAllByWeekStartOrderByPositionAsc(weekStart);
        List<PlayerChallengeProgress> progressRows = progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(
                weekStart
            );
        int totalChallenges = weeklyChallengeRepository
            .findAllByWeekStartOrderByIdAsc(weekStart)
            .size();

        Map<Long, List<PlayerChallengeProgress>> progressByPlayerId =
            progressRows.stream().collect(Collectors.groupingBy(
                progress -> progress.getPlayer().getId()
            ));

        Instant calculatedAt = scores.stream()
            .map(WeeklyPlayerScore::getCalculatedAt)
            .filter(Objects::nonNull)
            .max(Instant::compareTo)
            .orElse(null);

        List<CurrentRankingResponse.RankingEntryResponse> ranking = scores
            .stream()
            .map(score -> toCurrentEntry(
                score,
                totalChallenges,
                progressByPlayerId.getOrDefault(
                    score.getPlayer().getId(),
                    List.of()
                )
            ))
            .toList();

        return new CurrentRankingResponse(
            weekStart,
            weekStart.plusDays(6),
            calculatedAt,
            ranking
        );
    }

    /**
     * Returns finalized weekly ranking snapshots using week-based pagination.
     */
    @Override
    public PageResponse<RankingHistoryWeekResponse> findHistory(
        int page,
        int size
    ) {
        validatePagination(page, size);

        Page<LocalDate> weekPage = scoreRepository.findFinalizedWeekStarts(
            PageRequest.of(page, size)
        );
        List<WeeklyPlayerScore> scores = weekPage.isEmpty()
            ? List.of()
            : scoreRepository
            .findAllByWeekStartInOrderByWeekStartDescPositionAsc(
                weekPage.getContent()
            );
        Map<LocalDate, List<WeeklyPlayerScore>> scoresByWeek = scores.stream()
            .collect(Collectors.groupingBy(WeeklyPlayerScore::getWeekStart));

        List<RankingHistoryWeekResponse> content = weekPage.getContent()
            .stream()
            .map(weekStart -> toHistoryWeek(
                weekStart,
                scoresByWeek.getOrDefault(weekStart, List.of())
            ))
            .toList();

        return new PageResponse<>(
            content,
            weekPage.getNumber(),
            weekPage.getSize(),
            weekPage.getTotalElements(),
            weekPage.getTotalPages()
        );
    }

    /**
     * Maps one score and its progress rows to the current API contract.
     */
    private CurrentRankingResponse.RankingEntryResponse toCurrentEntry(
        WeeklyPlayerScore score,
        int totalChallenges,
        List<PlayerChallengeProgress> progressRows
    ) {
        List<CurrentRankingResponse.ChallengeProgressResponse> progress =
            progressRows.stream()
                .sorted(Comparator.comparing(
                    item -> item.getWeeklyChallenge().getId()
                ))
                .map(this::toChallengeProgress)
                .toList();
        Integer previousPosition = score.getPreviousPosition();
        Integer currentPosition = score.getPosition();
        int variation = previousPosition == null || currentPosition == null
            ? 0
            : previousPosition - currentPosition;

        return new CurrentRankingResponse.RankingEntryResponse(
            currentPosition,
            previousPosition,
            variation,
            new CurrentRankingResponse.PlayerRankingResponse(
                score.getPlayer().getId(),
                score.getPlayer().getDisplayName(),
                score.getPlayer().getPortrait(),
                score.getPlayer().getCompetitiveTier(),
                score.getPlayer().getRankRating()
            ),
            score.getChallengeDamage(),
            score.getCompletedChallenges(),
            totalChallenges,
            score.getMatchDamage(),
            score.getRegularityBonus(),
            score.getTeamBonus(),
            score.getActiveDays(),
            score.getTotalDamage(),
            progress
        );
    }

    /**
     * Maps one exact persisted progress row to the current API contract.
     */
    private CurrentRankingResponse.ChallengeProgressResponse toChallengeProgress(
        PlayerChallengeProgress progress
    ) {
        ChallengeDefinition definition = definitionParser.parse(
            progress.getWeeklyChallenge().getChallenge()
        );
        String metric = definition.conditions().stream()
            .map(condition -> condition.metric().name())
            .distinct()
            .collect(Collectors.joining(" + "));
        String unit = definition.conditions().size() == 1
            ? resolveUnit(definition.singleCondition().metric())
            : null;

        return new CurrentRankingResponse.ChallengeProgressResponse(
            progress.getWeeklyChallenge().getId(),
            progress.getWeeklyChallenge().getChallenge().getName(),
            metric,
            progress.getCurrentValue(),
            progress.getTargetValue(),
            unit,
            progress.isCompleted()
        );
    }

    /**
     * Maps one finalized week to its immutable history representation.
     */
    private RankingHistoryWeekResponse toHistoryWeek(
        LocalDate weekStart,
        List<WeeklyPlayerScore> scores
    ) {
        // Inactive players never consume a ranking slot (null position); they are excluded from
        // this history view entirely, unlike the current-week view where they still appear.
        List<WeeklyPlayerScore> orderedScores = scores.stream()
            .filter(score -> score.getPosition() != null)
            .sorted(Comparator.comparing(WeeklyPlayerScore::getPosition))
            .toList();
        Instant finalizedAt = orderedScores.stream()
            .map(WeeklyPlayerScore::getFinalizedAt)
            .filter(Objects::nonNull)
            .max(Instant::compareTo)
            .orElse(null);
        Long winnerPlayerId = orderedScores.stream()
            .filter(score -> Integer.valueOf(1).equals(score.getPosition()))
            .map(score -> score.getPlayer().getId())
            .findFirst()
            .orElse(null);

        List<RankingHistoryWeekResponse.FinalRankingEntryResponse> ranking =
            orderedScores.stream()
                .map(score ->
                    new RankingHistoryWeekResponse.FinalRankingEntryResponse(
                        score.getPosition(),
                        score.getPlayer().getId(),
                        score.getPlayer().getDisplayName(),
                        score.getChallengeDamage(),
                        score.getCompletedChallenges(),
                        score.getMatchDamage(),
                        score.getRegularityBonus(),
                        score.getTeamBonus(),
                        score.getActiveDays(),
                        score.getTotalDamage()
                    )
                )
                .toList();

        return new RankingHistoryWeekResponse(
            weekStart,
            weekStart.plusDays(6),
            finalizedAt,
            winnerPlayerId,
            ranking
        );
    }

    /**
     * Returns the display unit associated with one challenge metric.
     */
    private String resolveUnit(ChallengeMetric metric) {
        return switch (metric) {
            case DAMAGE_DEALT, SCORE -> "points";
            case KD -> "ratio";
            case PLAY_DAY -> "days";
            case MATCHES_PLAYED, MATCHES_WON -> "matches";
            case KILLS -> "kills";
            case ASSISTS -> "assists";
            case HEADSHOTS -> "headshots";
            case ROUNDS_PLAYED -> "rounds";
        };
    }

    /**
     * Validates public pagination parameters.
     */
    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException(
                "page must be greater than or equal to 0"
            );
        }
        if (size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw new InvalidRequestException(
                "size must be between 1 and " + MAXIMUM_PAGE_SIZE
            );
        }
    }
}
