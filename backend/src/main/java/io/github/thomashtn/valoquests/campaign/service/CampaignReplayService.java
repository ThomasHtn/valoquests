package io.github.thomashtn.valoquests.campaign.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignReplayInputs;
import io.github.thomashtn.valoquests.campaign.model.CampaignReplayResult;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.week.WeekCalendar;
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
 * <p>Nothing is ever incremented. The campaign is replayed whole from the matches and challenges it
 * is made of, which is what makes this safe to call from the end of every synchronization, from the
 * nightly tick and from the backoffice, in any order and any number of times.
 *
 * <p>Only a running campaign is replayed. One that has closed is frozen: it was settled one last
 * time on its way out, and a later change to the rules must not rewrite a score that has already
 * been read as final.
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
        Optional<Campaign> running = campaignRepository.findByStatusNot(CampaignStatus.CLOSED)
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
     * <p>Today's matches are played but today's Sunday is not settled: a settlement written while
     * the Sunday is still being played would strike the base, spend the stocks and open the mission
     * report on figures that the evening's matches are about to change. A week is settled from the
     * day after its Sunday, which is when its matches are all in.
     *
     * @param campaign campaign to replay
     * @return what the replay produced
     */
    @Transactional
    public CampaignReplayResult replay(Campaign campaign) {
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
