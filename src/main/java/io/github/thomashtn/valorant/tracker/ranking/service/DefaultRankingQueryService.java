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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
     * Clock used to determine the active week consistently.
     */
    private final Clock clock;

    /**
     * Creates the ranking query service.
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
        Clock clock
    ) {
        this.scoreRepository = scoreRepository;
        this.progressRepository = progressRepository;
        this.weeklyChallengeRepository = weeklyChallengeRepository;
        this.definitionParser = definitionParser;
        this.clock = clock;
    }

    /**
     * Returns the current ranking and exact progress for every ranked player.
     */
    @Override
    public CurrentRankingResponse findCurrent() {
        LocalDate weekStart = resolveCurrentWeekStart();
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
        int variation = previousPosition == null
            ? 0
            : previousPosition - score.getPosition();

        return new CurrentRankingResponse.RankingEntryResponse(
            score.getPosition(),
            previousPosition,
            variation,
            new CurrentRankingResponse.PlayerRankingResponse(
                score.getPlayer().getId(),
                score.getPlayer().getDisplayName(),
                score.getPlayer().getPortrait(),
                score.getPlayer().getCompetitiveTier(),
                score.getPlayer().getRankRating()
            ),
            score.getPoints(),
            score.getCompletedChallenges(),
            totalChallenges,
            progress
        );
    }

    /**
     * Maps one exact persisted progress row to the current API contract.
     */
    private CurrentRankingResponse.ChallengeProgressResponse
    toChallengeProgress(PlayerChallengeProgress progress) {
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
        List<WeeklyPlayerScore> orderedScores = scores.stream()
            .sorted(Comparator.comparingInt(WeeklyPlayerScore::getPosition))
            .toList();
        Instant finalizedAt = orderedScores.stream()
            .map(WeeklyPlayerScore::getFinalizedAt)
            .filter(Objects::nonNull)
            .max(Instant::compareTo)
            .orElse(null);
        Long winnerPlayerId = orderedScores.stream()
            .filter(score -> score.getPosition() == 1)
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
                        score.getPoints(),
                        score.getCompletedChallenges()
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
     * Resolves the Monday beginning the current UTC calendar week.
     */
    private LocalDate resolveCurrentWeekStart() {
        return LocalDate.now(clock.withZone(ZoneOffset.UTC)).with(
            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
    }

    /**
     * Validates public pagination parameters.
     */
    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException(
                "page must be greater than or equal to 0"
            );
        }
        if (size < 1 || size > MAXIMUM_PAGE_SIZE) {
            throw new IllegalArgumentException(
                "size must be between 1 and " + MAXIMUM_PAGE_SIZE
            );
        }
    }
}
