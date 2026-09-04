package io.github.thomashtn.valoquests.campaign.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayerDay;
import io.github.thomashtn.valoquests.campaign.model.WeekChallengeYield;
import io.github.thomashtn.valoquests.campaign.model.WeeklyTitle;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerDayRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Awards the four weekly honours from what the roster produced that week.
 *
 * <p>A tie awards nothing. Two operators who both did the most are not both the most, and a title
 * that can be shared stops meaning anything the first time it is.
 *
 * <p>Reads the stored operator days rather than re-pricing the week: the replay already wrote
 * exactly these figures, and reading them twice from two different places is how two screens end up
 * disagreeing about the same week.
 */
@Service
@Transactional(readOnly = true)
public class WeeklyTitleResolver {

    /**
     * Repository holding the campaign's per-operator days.
     */
    private final CampaignPlayerDayRepository playerDayRepository;

    /**
     * Repository holding the campaign's frozen roster.
     */
    private final CampaignPlayerRepository campaignPlayerRepository;

    /**
     * Reader reporting what each operator's challenges brought back.
     */
    private final CampaignChallengeReader challengeReader;

    /**
     * Creates the weekly title resolver.
     *
     * @param playerDayRepository      campaign player day repository
     * @param campaignPlayerRepository campaign roster repository
     * @param challengeReader          campaign challenge reader
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public WeeklyTitleResolver(
        CampaignPlayerDayRepository playerDayRepository,
        CampaignPlayerRepository campaignPlayerRepository,
        CampaignChallengeReader challengeReader
    ) {
        this.playerDayRepository = playerDayRepository;
        this.campaignPlayerRepository = campaignPlayerRepository;
        this.challengeReader = challengeReader;
    }

    /**
     * Awards one week's titles.
     *
     * @param campaign  campaign the week belongs to
     * @param weekStart Monday identifying the week
     * @return the holder of each title, titles nobody won outright omitted
     */
    public Map<WeeklyTitle, Long> resolve(Campaign campaign, LocalDate weekStart) {
        List<CampaignPlayerDay> days = playerDayRepository.findAllByCampaignIdAndDayBetweenOrderByDayAsc(
            campaign.getId(),
            weekStart,
            weekStart.plusDays(6)
        );

        Map<Long, Integer> components = new HashMap<>();
        Map<Long, Integer> food = new HashMap<>();
        Map<Long, Integer> streak = new HashMap<>();

        for (CampaignPlayerDay day : days) {
            long playerId = day.getPlayer().getId();
            components.merge(playerId, day.getComponents(), Integer::sum);
            food.merge(playerId, day.getFood(), Integer::sum);
            streak.merge(playerId, day.getStreakDays(), Math::max);
        }

        Map<WeeklyTitle, Long> titles = new EnumMap<>(WeeklyTitle.class);
        award(titles, WeeklyTitle.MECHANIC, components);
        award(titles, WeeklyTitle.QUARTERMASTER, food);
        award(titles, WeeklyTitle.REGULAR, streak);
        award(titles, WeeklyTitle.SCOUT, completions(campaign, weekStart));

        return titles;
    }

    /**
     * Reads how many challenges each operator validated that week.
     *
     * @param campaign  campaign the week belongs to
     * @param weekStart Monday identifying the week
     * @return validations per operator
     */
    private Map<Long, Integer> completions(Campaign campaign, LocalDate weekStart) {
        Set<Long> roster = campaignPlayerRepository.findAllByCampaignIdOrderByPlayerIdAsc(campaign.getId())
            .stream()
            .map(member -> member.getPlayer().getId())
            .collect(Collectors.toUnmodifiableSet());

        return challengeReader.read(campaign, roster)
            .getOrDefault(campaign.weekIndexOf(weekStart), WeekChallengeYield.NONE)
            .completionsByPlayer();
    }

    /**
     * Awards one title to the single operator holding the highest figure, if there is one.
     *
     * @param titles  titles awarded so far
     * @param title   title being awarded
     * @param figures figure per operator
     */
    private void award(Map<WeeklyTitle, Long> titles, WeeklyTitle title, Map<Long, Integer> figures) {
        int best = figures.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        if (best <= 0) {
            return;
        }

        List<Long> leaders = figures.entrySet().stream()
            .filter(entry -> entry.getValue() == best)
            .map(Map.Entry::getKey)
            .toList();

        if (leaders.size() == 1) {
            titles.put(title, leaders.getFirst());
        }
    }
}
