package io.github.thomashtn.valoquests.ranking.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeMetric;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.ranking.dto.CurrentRankingResponse;
import io.github.thomashtn.valoquests.ranking.dto.DailyRankingResponse;
import io.github.thomashtn.valoquests.ranking.dto.RankingHistoryWeekResponse;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.scoring.model.DailyMatchDamage;
import io.github.thomashtn.valoquests.scoring.service.DailyMatchDamageReader;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.github.thomashtn.valoquests.shared.util.PaginationGuard;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
     * Repository listing the roster the day's board draws a line for.
     */
    private final PlayerRepository playerRepository;

    /**
     * Reader pricing a day's matches, shared with the colony so one evening is worth the same on both.
     */
    private final DailyMatchDamageReader dailyDamageReader;

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
     * @param playerRepository          repository listing the roster
     * @param dailyDamageReader         reader pricing a day's matches
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
        PlayerRepository playerRepository,
        DailyMatchDamageReader dailyDamageReader,
        WeekCalendar weekCalendar
    ) {
        this.scoreRepository = scoreRepository;
        this.progressRepository = progressRepository;
        this.weeklyChallengeRepository = weeklyChallengeRepository;
        this.definitionParser = definitionParser;
        this.playerRepository = playerRepository;
        this.dailyDamageReader = dailyDamageReader;
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
        PaginationGuard.assertValidPageRequest(page, size);

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
     * Returns one day's ranking, priced on demand from the matches that day holds.
     *
     * <p>Read for the day <b>and the one before it</b> in a single pass, because the variation is the
     * point of this scale: a day's total on its own says nothing without last night to hold it against.
     * The reader walks whole weeks either way, so asking for the pair costs the same query as asking
     * for one — and it is what keeps a Monday comparing against the Sunday of the week before rather
     * than against nothing.
     *
     * <p>Every player of the roster gets a line, archived ones aside, whether they played or not.
     * A player who did not play is a zero on the board, and a zero on an evening the rest of the squad
     * played is exactly the thing this scale exists to show.
     *
     * <p>The two counts are the exception: they are a turnout, and a turnout is only readable against
     * the squad the board actually ranks. Counting a deactivated player who played would report a
     * presence over a board that has no slot to show it on.
     */
    @Override
    public DailyRankingResponse findDaily(LocalDate day) {
        LocalDate target = day == null ? weekCalendar.today() : day;
        LocalDate previous = target.minusDays(1);

        Set<PlayerStatus> statuses = EnumSet.complementOf(EnumSet.of(PlayerStatus.ARCHIVED));
        DailyMatchDamage damage = dailyDamageReader.read(statuses, previous, target);
        Map<Long, Integer> damageOnDay = damage.weightedDamageOn(target);
        Map<Long, Integer> damageOnPrevious = damage.weightedDamageOn(previous);

        List<Player> roster = playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED);
        List<Player> ordered = new ArrayList<>(roster);
        ordered.sort(dailyComparator(damageOnDay));

        List<DailyRankingResponse.DailyRankingEntryResponse> ranking = new ArrayList<>();
        int position = 1;
        for (Player player : ordered) {
            int matchDamage = damageOnDay.getOrDefault(player.getId(), 0);
            int previousDamage = damageOnPrevious.getOrDefault(player.getId(), 0);

            // Same rule as the weekly board: a non-competitive player is scored and shown, but never
            // consumes a ranking slot.
            ranking.add(new DailyRankingResponse.DailyRankingEntryResponse(
                player.isCompetitive() ? position++ : null,
                player.getId(),
                player.getDisplayName(),
                player.getPortrait(),
                matchDamage,
                previousDamage,
                matchDamage - previousDamage
            ));
        }

        // Both counts read the same predicate as the positions above, so the numerator, the
        // denominator and the slots on the board can never describe three different squads.
        List<Player> competitors = ordered.stream()
            .filter(Player::isCompetitive)
            .toList();
        long played = competitors.stream()
            .filter(player -> damageOnDay.getOrDefault(player.getId(), 0) > 0)
            .count();

        return new DailyRankingResponse(target, previous, (int) played, competitors.size(), ranking);
    }

    /**
     * Orders a day's board: best day first, ties broken on the player identifier so the order is
     * stable across two calls made on the same unchanged day.
     *
     * @param damageOnDay weighted damage of the day, indexed by player identifier
     * @return the day's comparator
     */
    private Comparator<Player> dailyComparator(Map<Long, Integer> damageOnDay) {
        return Comparator
            .comparingInt((Player player) -> damageOnDay.getOrDefault(player.getId(), 0))
            .reversed()
            .thenComparing(Player::getId);
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
            progress.getWeeklyChallenge()
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
            case ACS, ADR -> "per round";
            case HEADSHOT_RATE -> "ratio";
            case PLAY_DAY -> "days";
            case MATCHES_PLAYED, MATCHES_WON -> "matches";
            case KILLS -> "kills";
            case ASSISTS -> "assists";
            case HEADSHOTS -> "headshots";
            case ROUNDS_PLAYED -> "rounds";
        };
    }
}
