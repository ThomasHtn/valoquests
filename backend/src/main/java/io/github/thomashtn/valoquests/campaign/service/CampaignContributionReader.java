package io.github.thomashtn.valoquests.campaign.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayerDay;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignContribution;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.WeekChallengeYield;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerDayRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.ranking.service.WeeklyTitleResolver;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads what one operator has brought to the campaign in progress.
 *
 * <p>Reads only what the replay and the ranking already wrote: the operator's stored days, the
 * weeks' settlements and the weekly rows. Re-pricing any of it here would be a second answer to a
 * question already answered, and the profile would then disagree with the base it feeds.
 *
 * <p>An operator absent from the roster gets nothing, even if they played: a campaign was sized on
 * the players it froze, and a match from anyone else never reached its guardians.
 */
@Service
@Transactional(readOnly = true)
public class CampaignContributionReader {

    /**
     * Repository resolving the campaign in progress.
     */
    private final CampaignRepository campaignRepository;

    /**
     * Repository holding the campaign's frozen roster.
     */
    private final CampaignPlayerRepository campaignPlayerRepository;

    /**
     * Repository holding the campaign's per-operator days.
     */
    private final CampaignPlayerDayRepository playerDayRepository;

    /**
     * Repository holding the campaign's weeks and their settlements.
     */
    private final CampaignWeekRepository weekRepository;

    /**
     * Reader reporting what each operator's challenges brought back.
     */
    private final CampaignChallengeReader challengeReader;

    /**
     * Repository holding the weekly ranking rows the honours are read from.
     */
    private final WeeklyPlayerScoreRepository scoreRepository;

    /**
     * Resolver awarding a week's honours.
     */
    private final WeeklyTitleResolver titleResolver;

    /**
     * Creates the campaign contribution reader.
     *
     * @param campaignRepository       campaign repository
     * @param campaignPlayerRepository campaign roster repository
     * @param playerDayRepository      campaign player day repository
     * @param weekRepository           campaign week repository
     * @param challengeReader          campaign challenge reader
     * @param scoreRepository          weekly score repository
     * @param titleResolver            weekly title resolver
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public CampaignContributionReader(
        CampaignRepository campaignRepository,
        CampaignPlayerRepository campaignPlayerRepository,
        CampaignPlayerDayRepository playerDayRepository,
        CampaignWeekRepository weekRepository,
        CampaignChallengeReader challengeReader,
        WeeklyPlayerScoreRepository scoreRepository,
        WeeklyTitleResolver titleResolver
    ) {
        this.campaignRepository = campaignRepository;
        this.campaignPlayerRepository = campaignPlayerRepository;
        this.playerDayRepository = playerDayRepository;
        this.weekRepository = weekRepository;
        this.challengeReader = challengeReader;
        this.scoreRepository = scoreRepository;
        this.titleResolver = titleResolver;
    }

    /**
     * Reads one operator's contribution to the campaign in progress.
     *
     * @param playerId internal player identifier
     * @return the contribution, empty when no campaign is live or the operator is not on its roster
     */
    public Optional<CampaignContribution> read(long playerId) {
        return campaignRepository.findByStatusNot(CampaignStatus.CLOSED)
            .filter(campaign -> campaignPlayerRepository.existsByCampaignIdAndPlayerId(campaign.getId(), playerId))
            .map(campaign -> read(campaign, playerId));
    }

    /**
     * Sums one roster member's campaign.
     *
     * @param campaign campaign in progress
     * @param playerId internal player identifier
     * @return the contribution
     */
    private CampaignContribution read(Campaign campaign, long playerId) {
        List<CampaignPlayerDay> days = playerDayRepository
            .findAllByCampaignIdAndPlayerIdOrderByDayAsc(campaign.getId(), playerId);
        List<CampaignWeek> weeks = weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(campaign.getId());
        Map<Integer, WeekChallengeYield> yields = challengeReader.read(campaign, Set.of(playerId));

        int finishingBlows = (int) weeks.stream()
            .filter(week -> week.getDefeatedByPlayer() != null && week.getDefeatedByPlayer().getId() == playerId)
            .count();

        return new CampaignContribution(
            campaign.getId(),
            campaign.getNumber(),
            campaign.getStatus(),
            days.stream().mapToInt(CampaignPlayerDay::getDamage).sum(),
            days.stream().mapToInt(CampaignPlayerDay::getFood).sum(),
            days.stream().mapToInt(CampaignPlayerDay::getComponents).sum(),
            days.stream().mapToInt(CampaignPlayerDay::getMatchCount).sum(),
            (int) days.stream().filter(day -> day.getMatchCount() > 0).count(),
            days.stream().mapToInt(CampaignPlayerDay::getStreakDays).max().orElse(0),
            yields.values().stream().mapToInt(yield -> yield.completionsByPlayer().getOrDefault(playerId, 0)).sum(),
            yields.values().stream().mapToInt(yield -> yield.survivorsByPlayer().getOrDefault(playerId, 0)).sum(),
            finishingBlows,
            titlesOf(weeks.stream().map(CampaignWeek::getWeekStart).toList(), playerId)
        );
    }

    /**
     * Counts how many times each honour went to one operator over the campaign's weeks.
     *
     * <p>The week in progress counts as it stands: a title is provisional until Sunday, like every
     * other figure of the week.
     *
     * @param weekStarts Mondays of the campaign's weeks
     * @param playerId   internal player identifier
     * @return the count per title, titles never won omitted
     */
    private Map<WeeklyTitle, Integer> titlesOf(List<LocalDate> weekStarts, long playerId) {
        Map<LocalDate, List<WeeklyPlayerScore>> scoresByWeek = scoreRepository
            .findAllByWeekStartInOrderByWeekStartDescPositionAsc(weekStarts)
            .stream()
            .collect(Collectors.groupingBy(WeeklyPlayerScore::getWeekStart));

        Map<WeeklyTitle, Integer> titles = new EnumMap<>(WeeklyTitle.class);
        for (List<WeeklyPlayerScore> scores : scoresByWeek.values()) {
            titleResolver.resolve(scores).forEach((title, holder) -> {
                if (holder == playerId) {
                    titles.merge(title, 1, Integer::sum);
                }
            });
        }

        return titles;
    }
}
