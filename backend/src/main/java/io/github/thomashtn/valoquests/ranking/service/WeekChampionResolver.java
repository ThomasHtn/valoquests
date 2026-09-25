package io.github.thomashtn.valoquests.ranking.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Names the champion: the operator who finished alone in first place of a week played in a campaign.
 *
 * <p>The champion of the latest finalized week reigns until the next one is finalized, and holds no
 * other title meanwhile.
 */
@Component
public class WeekChampionResolver {

    /**
     * Repository used to read weekly ranking rows.
     */
    private final WeeklyPlayerScoreRepository scoreRepository;

    /**
     * Repository telling which weeks a campaign covered.
     */
    private final CampaignRepository campaignRepository;

    /**
     * Creates the champion resolver.
     *
     * @param scoreRepository    weekly score repository
     * @param campaignRepository campaign repository
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public WeekChampionResolver(WeeklyPlayerScoreRepository scoreRepository, CampaignRepository campaignRepository) {
        this.scoreRepository = scoreRepository;
        this.campaignRepository = campaignRepository;
    }

    /**
     * Names one week's champion.
     *
     * @param weekStart Monday identifying the week
     * @param scores    the week's rows
     * @return the champion's player id, or {@code null} between campaigns or on a shared first place
     */
    public Long championOf(LocalDate weekStart, List<WeeklyPlayerScore> scores) {
        List<Long> leaders = scores.stream()
            .filter(score -> Integer.valueOf(1).equals(score.getPosition()))
            .map(score -> score.getPlayer().getId())
            .toList();

        return leaders.size() == 1 && coveredByCampaign(weekStart) ? leaders.getFirst() : null;
    }

    /**
     * Names the reigning champion, the one of the latest finalized week.
     *
     * @return the champion's player id, or {@code null} when that week crowned nobody
     */
    public Long reigningChampion() {
        return scoreRepository.findFinalizedWeekStarts(PageRequest.of(0, 1)).stream()
            .findFirst()
            .map(weekStart -> championOf(
                weekStart,
                scoreRepository.findAllByWeekStartOrderByPositionAscPlayerIdAsc(weekStart)
            ))
            .orElse(null);
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
