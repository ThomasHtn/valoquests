package io.github.thomashtn.valoquests.campaign.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.CampaignRuleset;
import io.github.thomashtn.valoquests.campaign.dto.CampaignPlayerDayResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignTodayResponse;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignDailySnapshot;
import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayerDay;
import io.github.thomashtn.valoquests.campaign.repository.CampaignDailySnapshotRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerDayRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.ranking.service.WeekChampionResolver;
import io.github.thomashtn.valoquests.ranking.service.WeeklyTitleResolver;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads one day of a campaign, player by player.
 *
 * <p>Reads only what the replay already wrote. Re-pricing the day here would be a second answer to
 * a question the campaign has already answered, and two answers to the same question is how a
 * squad table ends up disagreeing with the base it feeds.
 *
 * <p>The week's titles are read here too, because the day's squad table shows them beside each
 * player. They come from the ranking rows as they stand, resolved by the same resolvers the ranking
 * uses, so the two screens can never award a title differently.
 */
@Service
@Transactional(readOnly = true)
public class CampaignDayReader {

    /**
     * Orders the day's players by what they brought in, most first.
     */
    private static final Comparator<CampaignPlayerDay> MOST_PRODUCTIVE_FIRST = Comparator
        .comparingInt(CampaignPlayerDay::getDamage).reversed()
        .thenComparing(day -> day.getPlayer().getId());

    /**
     * Repository holding the campaign's per-player days.
     */
    private final CampaignPlayerDayRepository playerDayRepository;

    /**
     * Repository holding the campaign's days.
     */
    private final CampaignDailySnapshotRepository snapshotRepository;

    /**
     * Repository holding the week's ranking rows, the honours are read from.
     */
    private final WeeklyPlayerScoreRepository scoreRepository;

    /**
     * Resolver awarding the week's honours.
     */
    private final WeeklyTitleResolver titleResolver;

    /**
     * Resolver naming the reigning champion, who holds no weekly title.
     */
    private final WeekChampionResolver championResolver;

    /**
     * Creates the campaign day reader.
     *
     * @param playerDayRepository campaign player day repository
     * @param snapshotRepository  campaign daily snapshot repository
     * @param scoreRepository     weekly score repository
     * @param titleResolver       weekly title resolver
     * @param championResolver    week champion resolver
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public CampaignDayReader(
        CampaignPlayerDayRepository playerDayRepository,
        CampaignDailySnapshotRepository snapshotRepository,
        WeeklyPlayerScoreRepository scoreRepository,
        WeeklyTitleResolver titleResolver,
        WeekChampionResolver championResolver
    ) {
        this.playerDayRepository = playerDayRepository;
        this.snapshotRepository = snapshotRepository;
        this.scoreRepository = scoreRepository;
        this.titleResolver = titleResolver;
        this.championResolver = championResolver;
    }

    /**
     * Reads one day of one campaign.
     *
     * @param campaign  campaign to read
     * @param day       calendar day
     * @param weekStart Monday of the week the honours are read over
     * @return the day, empty of players when nobody has played it
     */
    public CampaignTodayResponse read(Campaign campaign, LocalDate day, LocalDate weekStart) {
        List<CampaignPlayerDay> playerDays = playerDayRepository
            .findAllByCampaignIdAndDay(campaign.getId(), day)
            .stream()
            .sorted(MOST_PRODUCTIVE_FIRST)
            .toList();

        int upkeep = snapshotRepository.findByCampaignIdAndDay(campaign.getId(), day)
            .map(CampaignDailySnapshot::getPopulation)
            .map(population -> (int) Math.round(CampaignRuleset.dailyUpkeep(population.doubleValue())))
            .orElse(0);

        int food = playerDays.stream().mapToInt(CampaignPlayerDay::getFood).sum();
        int components = playerDays.stream().mapToInt(CampaignPlayerDay::getComponents).sum();

        return new CampaignTodayResponse(
            day,
            playerDays.stream().mapToInt(CampaignPlayerDay::getDamage).sum(),
            food,
            components,
            playerDays.size(),
            campaign.getRosterSize(),
            upkeep,
            components / CampaignRuleset.COMPONENTS_PER_RESCUE,
            food / CampaignRuleset.FOOD_PER_RESCUE,
            playerDays.stream().map(this::toResponse).toList(),
            titleResolver.resolve(
                scoreRepository.findAllByWeekStartOrderByPositionAscPlayerIdAsc(weekStart),
                null,
                championResolver.reigningChampion()
            )
        );
    }

    /**
     * Maps one stored player day to what the site shows.
     *
     * @param day stored player day
     * @return the response row
     */
    private CampaignPlayerDayResponse toResponse(CampaignPlayerDay day) {
        Player player = day.getPlayer();

        return new CampaignPlayerDayResponse(
            player.getId(),
            player.getGameName(),
            player.getTagLine(),
            day.getDamage(),
            day.getFood(),
            day.getComponents(),
            day.getMatchCount(),
            day.getReducedMatchCount(),
            day.getPlayedDays(),
            day.getStreakBonusPercent()
        );
    }
}
