package io.github.thomashtn.valoquests.campaign.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.dto.CampaignForecastResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignHistoryResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignTodayResponse;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignDailySnapshot;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.repository.CampaignDailySnapshotRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the campaign from what the replay stored, and nothing else.
 *
 * <p>Never computes a base of its own. Every figure here was written by a replay, so a page view
 * and the campaign it displays can never drift apart, however many times the page is refreshed.
 */
@Service
@Transactional(readOnly = true)
public class DefaultCampaignQueryService implements CampaignQueryService {

    /**
     * Repository resolving the campaign to show.
     */
    private final CampaignRepository campaignRepository;

    /**
     * Repository holding the campaign's weeks.
     */
    private final CampaignWeekRepository weekRepository;

    /**
     * Repository holding the campaign's days.
     */
    private final CampaignDailySnapshotRepository snapshotRepository;

    /**
     * Reader assembling the day in progress.
     */
    private final CampaignDayReader dayReader;

    /**
     * Calendar resolving today and the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the campaign query service.
     *
     * @param campaignRepository campaign repository
     * @param weekRepository     campaign week repository
     * @param snapshotRepository campaign daily snapshot repository
     * @param dayReader          campaign day reader
     * @param weekCalendar       week calendar
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public DefaultCampaignQueryService(
        CampaignRepository campaignRepository,
        CampaignWeekRepository weekRepository,
        CampaignDailySnapshotRepository snapshotRepository,
        CampaignDayReader dayReader,
        WeekCalendar weekCalendar
    ) {
        this.campaignRepository = campaignRepository;
        this.weekRepository = weekRepository;
        this.snapshotRepository = snapshotRepository;
        this.dayReader = dayReader;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Returns the campaign in force, or the last closed one, or nothing.
     *
     * @return the campaign
     */
    @Override
    public CampaignResponse currentCampaign() {
        LocalDate today = weekCalendar.today();
        Optional<Campaign> shown = campaignRepository.findShown();

        if (shown.isEmpty()) {
            return CampaignResponse.none(today);
        }

        Campaign campaign = shown.orElseThrow();
        List<CampaignWeek> weeks = weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(campaign.getId());
        List<CampaignDailySnapshot> days = snapshotRepository.findAllByCampaignIdOrderByDayAsc(campaign.getId());

        Integer currentWeekIndex = currentWeekIndex(campaign, today);

        return new CampaignResponse(
            campaign.getId(),
            campaign.getStatus(),
            campaign.getNumber(),
            campaign.getDifficulty(),
            campaign.reference(),
            campaign.getRosterSize(),
            campaign.getFirstWeekStart(),
            campaign.getLastWeekStart(),
            today,
            currentWeekIndex,
            CampaignResponseMapper.base(days),
            forecast(campaign, weeks, days, today),
            CampaignResponseMapper.weeks(weeks, days, currentWeekIndex),
            CampaignResponseMapper.totals(weeks, days)
        );
    }

    /**
     * Returns the day in progress.
     *
     * @return today
     */
    @Override
    public CampaignTodayResponse today() {
        LocalDate today = weekCalendar.today();

        return campaignRepository.findShown()
            .filter(campaign -> campaign.getStatus() == CampaignStatus.RUNNING)
            .map(campaign -> dayReader.read(campaign, today, weekCalendar.weekStartOf(today)))
            .orElseGet(() -> CampaignTodayResponse.none(today));
    }

    /**
     * Returns the closed campaigns, most recent first.
     *
     * @return the campaign history
     */
    @Override
    public List<CampaignHistoryResponse> history() {
        return campaignRepository.findAllClosed().stream()
            .map(this::toHistoryResponse)
            .toList();
    }

    /**
     * Places today inside the campaign's ten weeks.
     *
     * @param campaign campaign shown
     * @param today    calendar day
     * @return the one-based week in progress, {@code null} before the campaign starts
     */
    private Integer currentWeekIndex(Campaign campaign, LocalDate today) {
        if (today.isBefore(campaign.getFirstWeekStart())) {
            return null;
        }

        return campaign.scheduleWeekIndexOf(weekCalendar.weekStartOf(today));
    }

    /**
     * Forecasts the Sunday of the week in progress from the base as it stands.
     *
     * <p>Only while a running campaign is inside one of its weeks and that week is not settled yet:
     * before the first Monday there is nothing to extract from, and once Sunday is settled the week
     * itself carries the real figures.
     *
     * @param campaign campaign shown
     * @param weeks    its ten weeks
     * @param days     its replayed days, oldest first
     * @param today    calendar day
     * @return the forecast, {@code null} outside a week in progress
     */
    private CampaignForecastResponse forecast(
        Campaign campaign,
        List<CampaignWeek> weeks,
        List<CampaignDailySnapshot> days,
        LocalDate today
    ) {
        if (campaign.getStatus() != CampaignStatus.RUNNING || days.isEmpty()) {
            return null;
        }

        LocalDate weekStart = weekCalendar.weekStartOf(today);

        return weeks.stream()
            .filter(week -> week.getWeekStart().equals(weekStart))
            .filter(week -> !week.isSettled())
            .findFirst()
            .map(week -> CampaignResponseMapper.forecast(week, days.getLast()))
            .orElse(null);
    }

    /**
     * Maps one closed campaign to its history row.
     *
     * @param campaign closed campaign
     * @return the history response
     */
    private CampaignHistoryResponse toHistoryResponse(Campaign campaign) {
        List<CampaignWeek> weeks = weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(campaign.getId());
        List<CampaignDailySnapshot> days = snapshotRepository.findAllByCampaignIdOrderByDayAsc(campaign.getId());

        return CampaignResponseMapper.history(campaign, weeks, days);
    }
}
