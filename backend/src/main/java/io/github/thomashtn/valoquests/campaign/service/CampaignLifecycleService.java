package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.exception.CampaignLifecycleException;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.NewCampaign;
import io.github.thomashtn.valoquests.campaign.model.SquadCalibration;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.shared.exception.ResourceNotFoundException;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opens, starts, stops and closes campaigns.
 *
 * <p>A campaign is never opened on its own. The v1 campaign renewed itself at every rollover, which
 * meant a squad that stopped playing came back to a run six weeks in that they had already lost;
 * here an operator decides, and between two campaigns only the weekly ranking keeps turning.
 *
 * <p>Opening freezes two things at once: the roster and the calibration. Both are read on the day
 * the button is pressed and neither is ever read again, so a deactivation, an archive or a change
 * of form during the ten weeks cannot resize a guardian that has already been fought.
 */
@Service
public class CampaignLifecycleService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CampaignLifecycleService.class);

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
     * Repository resolving the roster to freeze.
     */
    private final PlayerRepository playerRepository;

    /**
     * Service measuring the squad at opening.
     */
    private final SquadCalibrationService calibrationService;

    /**
     * Factory building the campaign, its roster and its weeks.
     */
    private final CampaignFactory factory;

    /**
     * Calendar resolving today and the Monday a campaign starts on.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the campaign lifecycle service.
     *
     * @param campaignRepository       campaign repository
     * @param campaignPlayerRepository campaign roster repository
     * @param campaignWeekRepository   campaign week repository
     * @param playerRepository         player repository
     * @param calibrationService       squad calibration service
     * @param factory                  campaign factory
     * @param weekCalendar             week calendar
     */
    public CampaignLifecycleService(
        CampaignRepository campaignRepository,
        CampaignPlayerRepository campaignPlayerRepository,
        CampaignWeekRepository campaignWeekRepository,
        PlayerRepository playerRepository,
        SquadCalibrationService calibrationService,
        CampaignFactory factory,
        WeekCalendar weekCalendar
    ) {
        this.campaignRepository = campaignRepository;
        this.campaignPlayerRepository = campaignPlayerRepository;
        this.campaignWeekRepository = campaignWeekRepository;
        this.playerRepository = playerRepository;
        this.calibrationService = calibrationService;
        this.factory = factory;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Returns the campaign that is opened or running, if there is one.
     *
     * @return the live campaign, empty between two campaigns
     */
    @Transactional(readOnly = true)
    public Optional<Campaign> liveCampaign() {
        return campaignRepository.findByStatusNot(CampaignStatus.CLOSED);
    }

    /**
     * Measures the squad without committing to anything.
     *
     * <p>What the backoffice shows before the operator opens: the calibration is decided once and
     * never revised, so this is the only chance to notice that a player's history is thin.
     *
     * @return the calibration a campaign opened today would be given
     * @throws CampaignLifecycleException when no player is active
     */
    @Transactional(readOnly = true)
    public SquadCalibration previewCalibration() {
        return calibrationService.calibrate(activeRoster(), weekCalendar.today());
    }

    /**
     * Opens a campaign starting the Monday after today.
     *
     * @return the campaign, still {@link CampaignStatus#OPENED}
     * @throws CampaignLifecycleException when a campaign is already live or no player is active
     */
    @Transactional
    public Campaign open() {
        if (liveCampaign().isPresent()) {
            throw new CampaignLifecycleException(
                "A campaign is already opened or running. Stop it before opening another one."
            );
        }

        List<Player> roster = activeRoster();
        LocalDate today = weekCalendar.today();
        LocalDate firstWeekStart = weekCalendar.weekStartOf(today).plusWeeks(1);

        int number = campaignRepository.findFirstByOrderByNumberDesc()
            .map(campaign -> campaign.getNumber() + 1)
            .orElse(1);

        NewCampaign built = factory.build(number, roster, calibrationService.calibrate(roster, today), firstWeekStart);
        Campaign campaign = campaignRepository.save(built.campaign());
        campaignPlayerRepository.saveAll(built.roster());
        campaignWeekRepository.saveAll(built.weeks());

        LOGGER.info(
            "Campaign {} opened on {} for {} operator(s) at reference {} ({}), starting {}.",
            number,
            today,
            roster.size(),
            campaign.getReference(),
            campaign.getTier(),
            firstWeekStart
        );

        return campaign;
    }

    /**
     * Starts the opened campaign once its first Monday has come.
     *
     * <p>Idempotent, and driven by the calendar rather than by the rollover firing on time: a
     * rollover that missed its Monday still finds the campaign waiting on the next one.
     *
     * @return the campaign that just started, empty when none was waiting
     */
    @Transactional
    public Optional<Campaign> startIfDue() {
        Optional<Campaign> waiting = liveCampaign()
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
     * <p>Closed the day after its final day, never on it: the tenth Sunday is settled by the replay
     * of that very evening, and closing before it ran would freeze a score one week short.
     *
     * @param clock clock reading the closing instant
     * @return the campaign that just closed, empty when none was due
     */
    @Transactional
    public Optional<Campaign> closeIfComplete(Clock clock) {
        Optional<Campaign> finished = liveCampaign()
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
     * <p>Stopped on the day before today, not on today: today is still being played, and freezing a
     * campaign on a day that is not over would credit half a day and call it final.
     *
     * @param clock clock reading the closing instant
     * @return the campaign that was stopped
     * @throws CampaignLifecycleException when no campaign is live
     */
    @Transactional
    public Campaign stop(Clock clock) {
        Campaign campaign = liveCampaign().orElseThrow(() -> new CampaignLifecycleException(
            "No campaign is opened or running, so there is nothing to stop."
        ));

        campaign.setStatus(CampaignStatus.CLOSED);
        campaign.setClosedAt(clock.instant());
        campaign.setStoppedOn(weekCalendar.today().minusDays(1));
        campaignRepository.save(campaign);

        LOGGER.warn("Campaign {} was stopped early, frozen at {}.", campaign.getNumber(), campaign.getStoppedOn());

        return campaign;
    }

    /**
     * Deletes one campaign and everything it owns.
     *
     * @param id campaign identifier
     * @throws ResourceNotFoundException when no campaign owns the identifier
     */
    @Transactional
    public void delete(long id) {
        Campaign campaign = campaignRepository.findById(id).orElseThrow(
            () -> new ResourceNotFoundException("No campaign exists with id " + id)
        );

        campaignRepository.delete(campaign);
        LOGGER.warn("Campaign {} was deleted along with its weeks, roster and snapshots.", campaign.getNumber());
    }

    /**
     * Returns the players a campaign opened today would freeze.
     *
     * @return the active roster
     * @throws CampaignLifecycleException when no player is active
     */
    private List<Player> activeRoster() {
        List<Player> roster = playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE);

        if (roster.isEmpty()) {
            throw new CampaignLifecycleException(
                "No operator is active. Activate at least one before opening a campaign."
            );
        }

        return roster;
    }
}
