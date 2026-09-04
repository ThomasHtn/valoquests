package io.github.thomashtn.valoquests.campaign.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.CampaignRuleset;
import io.github.thomashtn.valoquests.campaign.dto.CampaignBaseResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignHistoryResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignTodayResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignTotalsResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignWeekResponse;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignDailySnapshot;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignSchedule;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.repository.CampaignDailySnapshotRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.week.WeekCalendar;
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
     * Percentage scale.
     */
    private static final int PERCENT = 100;

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
        Optional<Campaign> shown = campaignToShow();

        if (shown.isEmpty()) {
            return CampaignResponse.none(today);
        }

        Campaign campaign = shown.orElseThrow();
        List<CampaignWeek> weeks = weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(campaign.getId());
        List<CampaignDailySnapshot> days = snapshotRepository.findAllByCampaignIdOrderByDayAsc(campaign.getId());

        return new CampaignResponse(
            campaign.getStatus(),
            campaign.getNumber(),
            campaign.getTier(),
            campaign.getReference(),
            campaign.getRosterSize(),
            campaign.getFirstWeekStart(),
            campaign.getLastWeekStart(),
            today,
            currentWeekIndex(campaign, today),
            base(days),
            weeks.stream().map(this::toWeekResponse).toList(),
            totals(weeks, days)
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

        return campaignToShow()
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
        return campaignRepository.findAllByStatusOrderByNumberDesc(CampaignStatus.CLOSED).stream()
            .map(this::toHistoryResponse)
            .toList();
    }

    /**
     * Resolves which campaign the site shows: the live one, else the last closed one.
     *
     * @return the campaign to show, empty on a database that never had one
     */
    private Optional<Campaign> campaignToShow() {
        Optional<Campaign> live = campaignRepository.findByStatusNot(CampaignStatus.CLOSED);

        if (live.isPresent()) {
            return live;
        }

        return campaignRepository.findAllByStatusOrderByNumberDesc(CampaignStatus.CLOSED).stream().findFirst();
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

        return Math.clamp(campaign.weekIndexOf(weekCalendar.weekStartOf(today)), 1, CampaignSchedule.WEEK_COUNT);
    }

    /**
     * Reads the base from the last day the replay computed.
     *
     * @param days the campaign's days, oldest first
     * @return the base, all zeroes before the campaign's first day
     */
    private CampaignBaseResponse base(List<CampaignDailySnapshot> days) {
        if (days.isEmpty()) {
            return new CampaignBaseResponse(0, 0, 0, 0, 0, 0, 0);
        }

        CampaignDailySnapshot last = days.getLast();
        double population = last.getPopulation().doubleValue();
        double food = last.getFoodStock().doubleValue();
        double components = last.getComponentsStock().doubleValue();
        double upkeep = population * CampaignRuleset.FOOD_PER_INHABITANT_PER_DAY;
        double protectedFood = upkeep * CampaignRuleset.PROTECTED_FOOD_DAYS;

        return new CampaignBaseResponse(
            (int) Math.round(population),
            (int) Math.round(food),
            (int) Math.round(components),
            (int) Math.round(upkeep),
            (int) Math.round(protectedFood),
            (int) Math.floor(components / CampaignRuleset.COMPONENTS_PER_RESCUE),
            (int) Math.floor(Math.max(0, food - protectedFood) / CampaignRuleset.FOOD_PER_RESCUE)
        );
    }

    /**
     * Sums what the campaign has amounted to so far.
     *
     * @param weeks the campaign's weeks
     * @param days  the campaign's days
     * @return the totals
     */
    private CampaignTotalsResponse totals(List<CampaignWeek> weeks, List<CampaignDailySnapshot> days) {
        double lost = days.stream()
            .mapToDouble(day -> day.getFamineLoss().doubleValue() + day.getGuardianLoss().doubleValue())
            .sum();

        return new CampaignTotalsResponse(
            (int) weeks.stream().filter(CampaignWeek::isDefeated).count(),
            (int) weeks.stream().filter(CampaignWeek::isSettled).count(),
            weeks.stream().mapToInt(CampaignWeek::rescued).sum(),
            weeks.stream().mapToInt(CampaignWeek::getChallengeRescued).sum(),
            days.stream().mapToLong(CampaignDailySnapshot::getDamage).sum(),
            days.stream().mapToLong(CampaignDailySnapshot::getFoodGained).sum(),
            days.stream().mapToLong(CampaignDailySnapshot::getComponentsGained).sum(),
            (int) Math.round(lost)
        );
    }

    /**
     * Maps one stored week to what the map shows.
     *
     * @param week stored week
     * @return the week response
     */
    private CampaignWeekResponse toWeekResponse(CampaignWeek week) {
        return new CampaignWeekResponse(
            week.getWeekIndex(),
            week.getWeekStart(),
            week.getPlanetName(),
            week.getCategory(),
            week.getGuardian().getName(),
            week.getGuardian().getDescription(),
            week.getGuardianHitPoints(),
            week.getDamageDealt(),
            progressPercent(week),
            week.isDefeated(),
            week.getDefeatedAt(),
            week.getDefeatedByPlayer() == null ? null : week.getDefeatedByPlayer().getId(),
            week.getWoundedCount(),
            week.getChallengeRescued(),
            week.getExtractionRescued(),
            week.getFoodSpent(),
            week.getComponentsSpent(),
            week.getLimiter(),
            (int) Math.round(week.getBaseLoss().doubleValue()),
            week.isSettled()
        );
    }

    /**
     * Returns how far the squad got on one week's guardian.
     *
     * @param week week to read
     * @return the share of hit points removed, capped at a hundred
     */
    private int progressPercent(CampaignWeek week) {
        if (week.isDefeated() || week.getGuardianHitPoints() <= 0) {
            return week.isDefeated() ? PERCENT : 0;
        }

        return Math.min(PERCENT, week.getDamageDealt() * PERCENT / week.getGuardianHitPoints());
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

        return new CampaignHistoryResponse(
            campaign.getNumber(),
            campaign.getTier(),
            campaign.getReference(),
            campaign.getRosterSize(),
            campaign.getFirstWeekStart(),
            campaign.getLastWeekStart(),
            campaign.getStoppedOn(),
            (int) weeks.stream().filter(CampaignWeek::isDefeated).count(),
            days.isEmpty() ? 0 : (int) Math.round(days.getLast().getPopulation().doubleValue()),
            weeks.stream().mapToInt(CampaignWeek::rescued).sum(),
            weeklyPopulation(weeks, days)
        );
    }

    /**
     * Reads the base at the close of each settled week.
     *
     * @param weeks the campaign's weeks
     * @param days  the campaign's days
     * @return the curve, week one first
     */
    private List<Integer> weeklyPopulation(List<CampaignWeek> weeks, List<CampaignDailySnapshot> days) {
        return weeks.stream()
            .filter(CampaignWeek::isSettled)
            .map(week -> days.stream()
                .filter(day -> day.getDay().equals(week.settlementDay()))
                .findFirst()
                .map(day -> (int) Math.round(day.getPopulation().doubleValue()))
                .orElse(0))
            .toList();
    }
}
