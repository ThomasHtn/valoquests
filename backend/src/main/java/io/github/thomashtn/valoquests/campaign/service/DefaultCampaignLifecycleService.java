package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.exception.CampaignLifecycleException;
import io.github.thomashtn.valoquests.campaign.model.CampaignStartWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.NewCampaign;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.scoring.model.CampaignDifficulty;
import io.github.thomashtn.valoquests.shared.exception.ResourceNotFoundException;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default campaign lifecycle, driven by the calendar rather than by a scheduler firing on time.
 *
 * <p>A campaign never opens on its own. Opening freezes the roster and the difficulty, so later
 * changes cannot resize a guardian already fought.
 */
@Service
public class DefaultCampaignLifecycleService implements CampaignLifecycleService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCampaignLifecycleService.class);

    /**
     * Repository holding campaigns.
     */
    private final CampaignRepository campaignRepository;

    /**
     * Repository holding frozen rosters.
     */
    private final CampaignPlayerRepository campaignPlayerRepository;

    /**
     * Repository holding campaign weeks.
     */
    private final CampaignWeekRepository campaignWeekRepository;

    /**
     * Factory building the campaign, its frozen roster and its weeks.
     */
    private final CampaignFactory factory;

    /**
     * Calendar resolving today and the Monday a campaign starts on.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Rebuilds a campaign opened on the week in progress, and a stopped one up to its final day.
     */
    private final CampaignReplayService replayService;

    /**
     * Clock stamping a campaign's closing instant.
     */
    private final Clock clock;

    /**
     * Creates the campaign lifecycle service.
     *
     * @param campaignRepository       campaign repository
     * @param campaignPlayerRepository campaign roster repository
     * @param campaignWeekRepository   campaign week repository
     * @param factory                  campaign factory
     * @param weekCalendar             week calendar
     * @param replayService            campaign replay service
     * @param clock                    application clock
     */
    public DefaultCampaignLifecycleService(
        CampaignRepository campaignRepository,
        CampaignPlayerRepository campaignPlayerRepository,
        CampaignWeekRepository campaignWeekRepository,
        CampaignFactory factory,
        WeekCalendar weekCalendar,
        CampaignReplayService replayService,
        Clock clock
    ) {
        this.campaignRepository = campaignRepository;
        this.campaignPlayerRepository = campaignPlayerRepository;
        this.campaignWeekRepository = campaignWeekRepository;
        this.factory = factory;
        this.weekCalendar = weekCalendar;
        this.replayService = replayService;
        this.clock = clock;
    }

    /**
     * Opens a campaign on the chosen Monday.
     *
     * <p>{@link CampaignStartWeek#CURRENT_WEEK} starts {@link CampaignStatus#RUNNING} and is replayed
     * here to rebuild the days already over.
     *
     * @param difficulty difficulty the campaign is played at
     * @param startWeek  week the campaign starts on
     * @return the campaign, {@link CampaignStatus#RUNNING} when its first Monday is today or already past
     * @throws CampaignLifecycleException when a campaign is already live or no player is active
     */
    @Override
    @Transactional
    public Campaign open(CampaignDifficulty difficulty, CampaignStartWeek startWeek) {
        if (campaignRepository.findLive().isPresent()) {
            throw new CampaignLifecycleException(
                "A campaign is already opened or running. Stop it before opening another one."
            );
        }

        LocalDate today = weekCalendar.today();
        LocalDate weekStart = weekCalendar.weekStartOf(today);
        LocalDate firstWeekStart = startWeek == CampaignStartWeek.CURRENT_WEEK
            ? weekStart
            : weekStart.plusWeeks(1);

        int number = campaignRepository.findFirstByOrderByNumberDesc()
            .map(campaign -> campaign.getNumber() + 1)
            .orElse(1);

        NewCampaign built = factory.build(number, difficulty, firstWeekStart, today);
        Campaign campaign = campaignRepository.save(built.campaign());
        campaignPlayerRepository.saveAll(built.roster());
        campaignWeekRepository.saveAll(built.weeks());

        LOGGER.info(
            "Campaign {} opened on {} for {} player(s) at {} ({} reference), starting {}.",
            number,
            today,
            campaign.getRosterSize(),
            difficulty,
            campaign.reference(),
            firstWeekStart
        );

        if (campaign.getStatus() == CampaignStatus.RUNNING) {
            replayService.replay(campaign);
        }

        return campaign;
    }

    /**
     * Starts the opened campaign once its first Monday has come.
     *
     * <p>Idempotent and calendar-driven, so a missed Monday is caught up on the next run.
     *
     * @return the campaign that just started, empty when none was waiting
     */
    @Override
    @Transactional
    public Optional<Campaign> startIfDue() {
        Optional<Campaign> waiting = campaignRepository.findLive()
            .filter(campaign -> campaign.getStatus() == CampaignStatus.OPENED)
            .filter(campaign -> !weekCalendar.today().isBefore(campaign.getFirstWeekStart()));

        waiting.ifPresent(campaign -> {
            campaign.setStatus(CampaignStatus.RUNNING);
            campaignRepository.save(campaign);
            LOGGER.info("Campaign {} started on {}.", campaign.getNumber(), campaign.getFirstWeekStart());
        });

        return waiting;
    }

    /**
     * Closes the running campaign once its tenth Sunday has been settled.
     *
     * <p>Closed the day after its final day, never on it, so the tenth Sunday is settled first.
     *
     * @return the campaign that just closed, empty when none was due
     */
    @Override
    @Transactional
    public Optional<Campaign> closeIfComplete() {
        Optional<Campaign> finished = campaignRepository.findLive()
            .filter(campaign -> campaign.getStatus() == CampaignStatus.RUNNING)
            .filter(campaign -> weekCalendar.today().isAfter(campaign.finalDay()));

        finished.ifPresent(campaign -> {
            campaign.setStatus(CampaignStatus.CLOSED);
            campaign.setClosedAt(clock.instant());
            campaignRepository.save(campaign);
            LOGGER.info("Campaign {} closed after its final day {}.", campaign.getNumber(), campaign.finalDay());
        });

        return finished;
    }

    /**
     * Stops the live campaign now, freezing it at yesterday's base.
     *
     * <p>Stopped on yesterday, since today is still being played.
     *
     * @return the campaign that was stopped
     * @throws CampaignLifecycleException when no campaign is live
     */
    @Override
    @Transactional
    public Campaign stop() {
        Campaign campaign = campaignRepository.findLive().orElseThrow(() -> new CampaignLifecycleException(
            "No campaign is opened or running, so there is nothing to stop."
        ));

        boolean running = campaign.getStatus() == CampaignStatus.RUNNING;
        campaign.setStoppedOn(weekCalendar.today().minusDays(1));
        // The last replay ran through today: rebuild up to the stop day, or today's half day stays frozen in.
        if (running) {
            replayService.replay(campaign);
        }
        campaign.setStatus(CampaignStatus.CLOSED);
        campaign.setClosedAt(clock.instant());
        campaignRepository.save(campaign);

        LOGGER.warn("Campaign {} was stopped early, frozen at {}.", campaign.getNumber(), campaign.getStoppedOn());

        return campaign;
    }

    @Override
    @Transactional
    public void delete(long id) {
        Campaign campaign = campaignRepository.findById(id).orElseThrow(
            () -> new ResourceNotFoundException("No campaign exists with id " + id)
        );

        campaignRepository.delete(campaign);
        LOGGER.warn("Campaign {} was deleted along with its weeks, roster and snapshots.", campaign.getNumber());
    }
}
