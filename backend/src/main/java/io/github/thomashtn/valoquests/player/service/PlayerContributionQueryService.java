package io.github.thomashtn.valoquests.player.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.model.CampaignContribution;
import io.github.thomashtn.valoquests.campaign.service.CampaignContributionReader;
import io.github.thomashtn.valoquests.player.dto.PlayerContributionResponse;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.ranking.service.WeeklyTitleResolver;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads what one player brings to the squad, for the block heading their profile.
 *
 * <p>Two scales, both read from what is already stored: the week from the ranking row the last
 * rebuild wrote, the campaign from the days and settlements the last replay wrote.
 */
@Service
@Transactional(readOnly = true)
public class PlayerContributionQueryService {

    /**
     * Repository resolving the player.
     */
    private final PlayerRepository playerRepository;

    /**
     * Repository holding the weekly ranking rows.
     */
    private final WeeklyPlayerScoreRepository scoreRepository;

    /**
     * Resolver awarding a week's honours.
     */
    private final WeeklyTitleResolver titleResolver;

    /**
     * Reader summing the player's campaign.
     */
    private final CampaignContributionReader campaignContributionReader;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the player contribution query service.
     *
     * @param playerRepository           player repository
     * @param scoreRepository            weekly score repository
     * @param titleResolver              weekly title resolver
     * @param campaignContributionReader campaign contribution reader
     * @param weekCalendar               week calendar
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public PlayerContributionQueryService(
        PlayerRepository playerRepository,
        WeeklyPlayerScoreRepository scoreRepository,
        WeeklyTitleResolver titleResolver,
        CampaignContributionReader campaignContributionReader,
        WeekCalendar weekCalendar
    ) {
        this.playerRepository = playerRepository;
        this.scoreRepository = scoreRepository;
        this.titleResolver = titleResolver;
        this.campaignContributionReader = campaignContributionReader;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Reads one player's contribution.
     *
     * @param playerId internal player identifier
     * @return the contribution
     * @throws PlayerNotFoundException when no tracked player has that identifier
     */
    public PlayerContributionResponse findByPlayerId(long playerId) {
        if (!playerRepository.existsById(playerId)) {
            throw new PlayerNotFoundException(playerId);
        }

        LocalDate weekStart = weekCalendar.currentWeekStart();
        List<WeeklyPlayerScore> scores = scoreRepository.findAllByWeekStartOrderByPositionAsc(weekStart);
        Map<WeeklyTitle, Long> titles = titleResolver.resolve(scores);

        PlayerContributionResponse.WeekContributionResponse week = scores.stream()
            .filter(score -> score.getPlayer().getId() == playerId)
            .findFirst()
            .map(score -> toWeek(score, titles))
            .orElse(null);

        PlayerContributionResponse.CampaignContributionResponse campaign = campaignContributionReader
            .read(playerId)
            .map(PlayerContributionQueryService::toCampaign)
            .orElse(null);

        return new PlayerContributionResponse(playerId, week, campaign);
    }

    /**
     * Maps one ranking row to the player's week.
     *
     * @param score  the row
     * @param titles the week's honours
     * @return the week
     */
    private static PlayerContributionResponse.WeekContributionResponse toWeek(
        WeeklyPlayerScore score,
        Map<WeeklyTitle, Long> titles
    ) {
        List<WeeklyTitle> held = titles.entrySet().stream()
            .filter(entry -> entry.getValue().equals(score.getPlayer().getId()))
            .map(Map.Entry::getKey)
            .sorted()
            .toList();

        return new PlayerContributionResponse.WeekContributionResponse(
            score.getWeekStart(),
            score.getPosition(),
            score.getGuardianDamage(),
            score.getFood(),
            score.getComponents(),
            score.getMatchCount(),
            score.getActiveDays(),
            score.getStreakDays(),
            score.getChallengePoints(),
            score.getCompletedChallenges(),
            score.getCompletedDailyChallenges(),
            score.getTotalPoints(),
            held
        );
    }

    /**
     * Maps one campaign contribution to the API contract.
     *
     * @param contribution the contribution
     * @return the response block
     */
    private static PlayerContributionResponse.CampaignContributionResponse toCampaign(
        CampaignContribution contribution
    ) {
        return new PlayerContributionResponse.CampaignContributionResponse(
            contribution.campaignId(),
            contribution.campaignNumber(),
            contribution.status(),
            contribution.damage(),
            contribution.food(),
            contribution.components(),
            contribution.matchCount(),
            contribution.activeDays(),
            contribution.longestStreak(),
            contribution.completedChallenges(),
            contribution.survivorsRescued(),
            contribution.finishingBlows(),
            contribution.titles()
        );
    }
}
