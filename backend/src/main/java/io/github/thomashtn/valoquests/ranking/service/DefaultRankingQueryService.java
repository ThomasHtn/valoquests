package io.github.thomashtn.valoquests.ranking.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeSelectionRepository;
import io.github.thomashtn.valoquests.ranking.dto.CurrentRankingResponse;
import io.github.thomashtn.valoquests.ranking.dto.DailyRankingResponse;
import io.github.thomashtn.valoquests.ranking.dto.RankingHistoryWeekResponse;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import io.github.thomashtn.valoquests.shared.util.PaginationGuard;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides optimized read-only access to current, daily and historical rankings.
 */
@Service
@Transactional(readOnly = true)
public class DefaultRankingQueryService implements RankingQueryService {

    /**
     * Repository used to read weekly ranking rows.
     */
    private final WeeklyPlayerScoreRepository scoreRepository;

    /**
     * Repository counting the week's weekly challenges.
     */
    private final ChallengeSelectionRepository challengeSelectionRepository;

    /**
     * Reader pricing and ranking one day.
     */
    private final DailyRankingReader dailyRankingReader;

    /**
     * Resolver awarding a week's honours.
     */
    private final WeeklyTitleResolver titleResolver;

    /**
     * Calendar resolving the current week and day.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Resolver naming each week's champion.
     */
    private final WeekChampionResolver championResolver;

    /**
     * Creates the ranking query service.
     *
     * @param scoreRepository           weekly score repository
     * @param challengeSelectionRepository challenge selection repository
     * @param dailyRankingReader        daily ranking reader
     * @param titleResolver             weekly title resolver
     * @param weekCalendar              week calendar
     * @param championResolver          week champion resolver
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public DefaultRankingQueryService(
        WeeklyPlayerScoreRepository scoreRepository,
        ChallengeSelectionRepository challengeSelectionRepository,
        DailyRankingReader dailyRankingReader,
        WeeklyTitleResolver titleResolver,
        WeekCalendar weekCalendar,
        WeekChampionResolver championResolver
    ) {
        this.scoreRepository = scoreRepository;
        this.challengeSelectionRepository = challengeSelectionRepository;
        this.dailyRankingReader = dailyRankingReader;
        this.titleResolver = titleResolver;
        this.weekCalendar = weekCalendar;
        this.championResolver = championResolver;
    }

    @Override
    public CurrentRankingResponse findCurrent() {
        LocalDate weekStart = weekCalendar.currentWeekStart();
        LocalDate today = weekCalendar.today();
        List<WeeklyPlayerScore> scores = scoreRepository.findAllByWeekStartOrderByPositionAscPlayerIdAsc(weekStart);

        int totalChallenges = (int) challengeSelectionRepository.countByWeekStartAndCadence(
            weekStart,
            ChallengeCadence.WEEKLY
        );
        Map<WeeklyTitle, Long> titles = titleResolver.resolve(scores, null, championResolver.reigningChampion());

        List<CurrentRankingResponse.RankingEntryResponse> ranking = scores.stream()
            .map(score -> toCurrentEntry(score, totalChallenges, titlesOf(titles, score)))
            .toList();

        return new CurrentRankingResponse(
            weekStart,
            WeekCalendar.lastDayOf(weekStart),
            today,
            ranking
        );
    }

    @Override
    public PageResponse<RankingHistoryWeekResponse> findHistory(int page, int size) {
        PaginationGuard.assertValidPageRequest(page, size);

        Page<LocalDate> weekPage = scoreRepository.findFinalizedWeekStarts(PageRequest.of(page, size));
        List<WeeklyPlayerScore> scores = weekPage.isEmpty()
            ? List.of()
            : scoreRepository.findAllByWeekStartInOrderByWeekStartDescPositionAsc(weekPage.getContent());
        Map<LocalDate, List<WeeklyPlayerScore>> scoresByWeek = scores.stream()
            .collect(Collectors.groupingBy(WeeklyPlayerScore::getWeekStart));

        List<RankingHistoryWeekResponse> content = weekPage.getContent()
            .stream()
            .map(weekStart -> toHistoryWeek(weekStart, scoresByWeek.getOrDefault(weekStart, List.of())))
            .toList();

        return PageResponse.from(weekPage, content);
    }

    @Override
    public DailyRankingResponse findDaily(LocalDate day) {
        return dailyRankingReader.read(day == null ? weekCalendar.today() : day);
    }

    /**
     * Maps one row to the current API contract.
     *
     * @param score           the player's row
     * @param totalChallenges weekly challenges selected for the week
     * @param titles          honours the player holds
     * @return the entry
     */
    private CurrentRankingResponse.RankingEntryResponse toCurrentEntry(
        WeeklyPlayerScore score,
        int totalChallenges,
        List<WeeklyTitle> titles
    ) {
        Integer previousPosition = score.getPreviousPosition();
        Integer currentPosition = score.getPosition();
        int variation = previousPosition == null || currentPosition == null
            ? 0
            : previousPosition - currentPosition;

        return new CurrentRankingResponse.RankingEntryResponse(
            currentPosition,
            score.getPlayer().isCompetitive(),
            variation,
            new CurrentRankingResponse.PlayerRankingResponse(
                score.getPlayer().getId(),
                score.getPlayer().getDisplayName(),
                score.getPlayer().getPortrait(),
                score.getPlayer().getCompetitiveTier(),
                score.getPlayer().getRankRating()
            ),
            score.getGuardianDamage(),
            score.getFood(),
            score.getComponents(),
            score.getMatchCount(),
            score.getPlayedDays(),
            score.getChallengePoints(),
            score.getCompletedChallenges(),
            totalChallenges,
            score.getCompletedDailyChallenges(),
            score.getTotalPoints(),
            titles
        );
    }

    /**
     * Maps one finalized week to its immutable history representation.
     *
     * <p>Inactive players never consume a ranking slot; they are left out of the history entirely,
     * unlike the current-week view where they still appear.
     *
     * @param weekStart Monday identifying the week
     * @param scores    the week's rows
     * @return the week
     */
    private RankingHistoryWeekResponse toHistoryWeek(LocalDate weekStart, List<WeeklyPlayerScore> scores) {
        List<WeeklyPlayerScore> orderedScores = scores.stream()
            .filter(score -> score.getPosition() != null)
            .sorted(Comparator.comparing(WeeklyPlayerScore::getPosition))
            .toList();
        Long winnerPlayerId = championResolver.championOf(weekStart, orderedScores);
        Map<WeeklyTitle, Long> titles = titleResolver.resolve(
            orderedScores,
            winnerPlayerId,
            championResolver.championBefore(weekStart)
        );

        List<RankingHistoryWeekResponse.FinalRankingEntryResponse> ranking = orderedScores.stream()
            .map(score -> new RankingHistoryWeekResponse.FinalRankingEntryResponse(
                score.getPosition(),
                score.getPlayer().getId(),
                score.getPlayer().getDisplayName(),
                score.getGuardianDamage(),
                score.getChallengePoints(),
                score.getTotalPoints(),
                score.getCompletedChallenges(),
                score.getCompletedDailyChallenges(),
                score.getMatchCount(),
                score.getPlayedDays(),
                titlesOf(titles, score)
            ))
            .toList();

        return new RankingHistoryWeekResponse(
            weekStart,
            WeekCalendar.lastDayOf(weekStart),
            winnerPlayerId,
            ranking
        );
    }

    /**
     * Lists the honours one row holds.
     *
     * @param titles the week's honours
     * @param score  the row
     * @return the titles awarded to that row's player, in declaration order
     */
    private static List<WeeklyTitle> titlesOf(Map<WeeklyTitle, Long> titles, WeeklyPlayerScore score) {
        return titles.entrySet().stream()
            .filter(entry -> entry.getValue().equals(score.getPlayer().getId()))
            .map(Map.Entry::getKey)
            .sorted()
            .toList();
    }
}
