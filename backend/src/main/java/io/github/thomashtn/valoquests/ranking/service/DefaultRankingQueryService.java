package io.github.thomashtn.valoquests.ranking.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.ranking.dto.CurrentRankingResponse;
import io.github.thomashtn.valoquests.ranking.dto.DailyRankingResponse;
import io.github.thomashtn.valoquests.ranking.dto.RankingHistoryWeekResponse;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.ranking.service.RankingProgressMapper.WeekBoard;
import io.github.thomashtn.valoquests.shared.dto.PageResponse;
import io.github.thomashtn.valoquests.shared.util.PaginationGuard;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import io.github.thomashtn.valoquests.week.WeekConstants;
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
     * Mapper laying out each player's progress on the board.
     */
    private final RankingProgressMapper progressMapper;

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
     * Repository telling which weeks a campaign covered.
     */
    private final CampaignRepository campaignRepository;

    /**
     * Creates the ranking query service.
     *
     * @param scoreRepository    weekly score repository
     * @param progressMapper     ranking progress mapper
     * @param dailyRankingReader daily ranking reader
     * @param titleResolver      weekly title resolver
     * @param weekCalendar       week calendar
     * @param campaignRepository campaign repository
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public DefaultRankingQueryService(
        WeeklyPlayerScoreRepository scoreRepository,
        RankingProgressMapper progressMapper,
        DailyRankingReader dailyRankingReader,
        WeeklyTitleResolver titleResolver,
        WeekCalendar weekCalendar,
        CampaignRepository campaignRepository
    ) {
        this.scoreRepository = scoreRepository;
        this.progressMapper = progressMapper;
        this.dailyRankingReader = dailyRankingReader;
        this.titleResolver = titleResolver;
        this.weekCalendar = weekCalendar;
        this.campaignRepository = campaignRepository;
    }

    @Override
    public CurrentRankingResponse findCurrent() {
        LocalDate weekStart = weekCalendar.currentWeekStart();
        LocalDate today = weekCalendar.today();
        List<WeeklyPlayerScore> scores = scoreRepository.findAllByWeekStartOrderByPositionAscPlayerIdAsc(weekStart);

        WeekBoard board = progressMapper.forWeek(
            weekStart,
            today,
            scores.stream().map(score -> score.getPlayer().getId()).toList()
        );
        Map<WeeklyTitle, Long> titles = titleResolver.resolve(scores);

        Instant calculatedAt = scores.stream()
            .map(WeeklyPlayerScore::getCalculatedAt)
            .filter(Objects::nonNull)
            .max(Instant::compareTo)
            .orElse(null);

        List<CurrentRankingResponse.RankingEntryResponse> ranking = scores.stream()
            .map(score -> toCurrentEntry(score, board, titlesOf(titles, score)))
            .toList();

        return new CurrentRankingResponse(
            weekStart,
            weekStart.plusDays(WeekConstants.LAST_DAY_OFFSET),
            today,
            calculatedAt,
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

        return new PageResponse<>(
            content,
            weekPage.getNumber(),
            weekPage.getSize(),
            weekPage.getTotalElements(),
            weekPage.getTotalPages()
        );
    }

    @Override
    public DailyRankingResponse findDaily(LocalDate day) {
        return dailyRankingReader.read(day == null ? weekCalendar.today() : day);
    }

    /**
     * Maps one row and its board lines to the current API contract.
     *
     * @param score  the player's row
     * @param board  the week's board
     * @param titles honours the player holds
     * @return the entry
     */
    private CurrentRankingResponse.RankingEntryResponse toCurrentEntry(
        WeeklyPlayerScore score,
        WeekBoard board,
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
            previousPosition,
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
            score.getActiveDays(),
            score.getStreakDays(),
            score.getChallengePoints(),
            score.getCompletedChallenges(),
            board.weeklyChallengeCount(),
            score.getCompletedDailyChallenges(),
            score.getTotalPoints(),
            titles,
            board.of(score.getPlayer().getId())
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
        Map<WeeklyTitle, Long> titles = titleResolver.resolve(orderedScores);

        Instant finalizedAt = orderedScores.stream()
            .map(WeeklyPlayerScore::getFinalizedAt)
            .filter(Objects::nonNull)
            .max(Instant::compareTo)
            .orElse(null);
        // The champion is a campaign honour: none between campaigns, none on a shared first place.
        List<Long> leaders = orderedScores.stream()
            .filter(score -> Integer.valueOf(1).equals(score.getPosition()))
            .map(score -> score.getPlayer().getId())
            .toList();
        Long winnerPlayerId = leaders.size() == 1 && coveredByCampaign(weekStart) ? leaders.getFirst() : null;

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
                score.getActiveDays(),
                score.getStreakDays(),
                titlesOf(titles, score)
            ))
            .toList();

        return new RankingHistoryWeekResponse(
            weekStart,
            weekStart.plusDays(WeekConstants.LAST_DAY_OFFSET),
            finalizedAt,
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

    /**
     * Tells whether a week was played inside a campaign, running or already closed.
     *
     * @param weekStart Monday identifying the week
     * @return {@code true} when a campaign covered it
     */
    private boolean coveredByCampaign(LocalDate weekStart) {
        return campaignRepository.findAll().stream()
            .filter(campaign -> campaign.getStatus() != CampaignStatus.OPENED)
            .anyMatch(campaign -> covers(campaign, weekStart));
    }

    /**
     * Tells whether a campaign's weeks include one, a stopped campaign ending where it stopped.
     *
     * @param campaign  campaign to check
     * @param weekStart Monday identifying the week
     * @return {@code true} when the week is one of the campaign's
     */
    private static boolean covers(Campaign campaign, LocalDate weekStart) {
        LocalDate lastMonday = campaign.getStoppedOn() == null
            ? campaign.getLastWeekStart()
            : campaign.getStoppedOn();

        return !weekStart.isBefore(campaign.getFirstWeekStart()) && !weekStart.isAfter(lastMonday);
    }
}
