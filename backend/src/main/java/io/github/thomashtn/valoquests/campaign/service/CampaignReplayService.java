package io.github.thomashtn.valoquests.campaign.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignReplayInputs;
import io.github.thomashtn.valoquests.campaign.model.CampaignReplayResult;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rebuilds the campaign in progress from its first day, every time.
 *
 * <p>Idempotent, so safe to call in any order and any number of times. Only a running campaign is
 * replayed: a closed one is frozen.
 */
@Service
public class CampaignReplayService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignReplayService.class);

    /**
     * Repository resolving the campaign in progress.
     */
    private final CampaignRepository campaignRepository;

    /**
     * Repository holding the campaign's weeks.
     */
    private final CampaignWeekRepository weekRepository;

    /**
     * Assembler reading everything the replay consumes.
     */
    private final CampaignReplayInputAssembler assembler;

    /**
     * Engine computing the base day by day.
     */
    private final CampaignReplayEngine engine;

    /**
     * Writer replacing the campaign's stored rows.
     */
    private final CampaignReplayWriter writer;

    /**
     * Calendar resolving today.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the campaign replay service.
     *
     * @param campaignRepository campaign repository
     * @param weekRepository     campaign week repository
     * @param assembler          replay input assembler
     * @param engine             replay engine
     * @param writer             replay writer
     * @param weekCalendar       week calendar
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public CampaignReplayService(
        CampaignRepository campaignRepository,
        CampaignWeekRepository weekRepository,
        CampaignReplayInputAssembler assembler,
        CampaignReplayEngine engine,
        CampaignReplayWriter writer,
        WeekCalendar weekCalendar
    ) {
        this.campaignRepository = campaignRepository;
        this.weekRepository = weekRepository;
        this.assembler = assembler;
        this.engine = engine;
        this.writer = writer;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Replays the campaign in progress, if one is running.
     *
     * @return what the replay produced, empty when no campaign has started yet
     */
    @Transactional
    public Optional<CampaignReplayResult> replayRunningCampaign() {
        Optional<Campaign> running = campaignRepository.findLive()
            .filter(campaign -> campaign.getStatus() == CampaignStatus.RUNNING);

        if (running.isEmpty()) {
            LOGGER.debug("No campaign is running: there is nothing to replay.");

            return Optional.empty();
        }

        return Optional.of(replay(running.orElseThrow()));
    }

    /**
     * Replays one campaign up to today, or up to its final day once it is past.
     *
     * <p>Today's matches count, but a week is only settled from the day after its Sunday, once all
     * its matches are in.
     *
     * @param campaign campaign to replay
     * @return what the replay produced
     */
    @Transactional
    public CampaignReplayResult replay(Campaign campaign) {
        // Only the lock matters: the caller already holds the campaign.
        campaignRepository.lockById(campaign.getId());
        LocalDate today = weekCalendar.today();
        LocalDate lastDay = earlier(today, campaign.finalDay());
        LocalDate settledThrough = earlier(today.minusDays(1), campaign.finalDay());
        List<CampaignWeek> weeks = weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(campaign.getId());

        CampaignReplayInputs inputs = assembler.assemble(campaign, weeks, lastDay, settledThrough);
        CampaignReplayResult result = engine.replay(inputs.days(), inputs.weeks());
        writer.write(campaign, weeks, inputs, result);

        LOGGER.info(
            "Campaign {} replayed up to {}: {} day(s), {} week(s) settled, base at {}.",
            campaign.getNumber(),
            lastDay,
            result.days().size(),
            result.settlements().size(),
            Math.round(result.population())
        );

        return result;
    }

    /**
     * Returns the earlier of two days.
     *
     * @param left  first day
     * @param right second day
     * @return the earlier one
     */
    private LocalDate earlier(LocalDate left, LocalDate right) {
        return left.isBefore(right) ? left : right;
    }
}
