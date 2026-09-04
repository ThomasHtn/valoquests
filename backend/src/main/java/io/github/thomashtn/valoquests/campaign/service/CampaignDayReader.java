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
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads one day of a campaign, operator by operator.
 *
 * <p>Reads only what the replay already wrote. Re-pricing the day here would be a second answer to
 * a question the campaign has already answered, and two answers to the same question is how a
 * squad table ends up disagreeing with the base it feeds.
 */
@Service
@Transactional(readOnly = true)
public class CampaignDayReader {

    /**
     * Orders the day's operators by what they brought in, most first.
     */
    private static final Comparator<CampaignPlayerDay> MOST_PRODUCTIVE_FIRST = Comparator
        .comparingInt(CampaignPlayerDay::getDamage).reversed()
        .thenComparing(day -> day.getPlayer().getId());

    /**
     * Repository holding the campaign's per-operator days.
     */
    private final CampaignPlayerDayRepository playerDayRepository;

    /**
     * Repository holding the campaign's days.
     */
    private final CampaignDailySnapshotRepository snapshotRepository;

    /**
     * Resolver awarding the week's honours.
     */
    private final WeeklyTitleResolver titleResolver;

    /**
     * Creates the campaign day reader.
     *
     * @param playerDayRepository campaign player day repository
     * @param snapshotRepository  campaign daily snapshot repository
     * @param titleResolver       weekly title resolver
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public CampaignDayReader(
        CampaignPlayerDayRepository playerDayRepository,
        CampaignDailySnapshotRepository snapshotRepository,
        WeeklyTitleResolver titleResolver
    ) {
        this.playerDayRepository = playerDayRepository;
        this.snapshotRepository = snapshotRepository;
        this.titleResolver = titleResolver;
    }

    /**
     * Reads one day of one campaign.
     *
     * @param campaign  campaign to read
     * @param day       calendar day
     * @param weekStart Monday of the week the honours are read over
     * @return the day, empty of operators when nobody has played it
     */
    public CampaignTodayResponse read(Campaign campaign, LocalDate day, LocalDate weekStart) {
        List<CampaignPlayerDay> playerDays = playerDayRepository
            .findAllByCampaignIdAndDayBetweenOrderByDayAsc(campaign.getId(), day, day)
            .stream()
            .sorted(MOST_PRODUCTIVE_FIRST)
            .toList();

        int upkeep = snapshotRepository.findByCampaignIdAndDay(campaign.getId(), day)
            .map(CampaignDailySnapshot::getPopulation)
            .map(population -> (int) Math.round(
                population.doubleValue() * CampaignRuleset.FOOD_PER_INHABITANT_PER_DAY
            ))
            .orElse(0);

        return new CampaignTodayResponse(
            day,
            playerDays.stream().mapToInt(CampaignPlayerDay::getDamage).sum(),
            playerDays.stream().mapToInt(CampaignPlayerDay::getFood).sum(),
            playerDays.stream().mapToInt(CampaignPlayerDay::getComponents).sum(),
            playerDays.size(),
            campaign.getRosterSize(),
            upkeep,
            playerDays.stream().map(this::toResponse).toList(),
            titleResolver.resolve(campaign, weekStart)
        );
    }

    /**
     * Maps one stored operator day to what the site shows.
     *
     * @param day stored operator day
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
            day.getStreakDays(),
            day.getStreakBonusPercent()
        );
    }
}
