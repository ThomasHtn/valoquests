package io.github.thomashtn.valoquests.campaign.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.CampaignRuleset;
import io.github.thomashtn.valoquests.campaign.dto.CampaignBaseResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignForecastResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignHistoryResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignTodayResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignTotalsResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignWeekBaseResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignWeekResponse;
import io.github.thomashtn.valoquests.campaign.dto.FatalBlowResponse;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignDailySnapshot;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignSchedule;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.ExtractionEstimate;
import io.github.thomashtn.valoquests.campaign.repository.CampaignDailySnapshotRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.LocalDate;
import java.util.ArrayList;
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
     * Share of the base a guardian left standing at zero breakthrough would kill, in percent.
     */
    private static final int GUARDIAN_LOSS_PERCENT = (int) Math.round(CampaignRuleset.GUARDIAN_LOSS_RATE * PERCENT);

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

        Integer currentWeekIndex = currentWeekIndex(campaign, today);

        return new CampaignResponse(
            campaign.getId(),
            campaign.getStatus(),
            campaign.getNumber(),
            campaign.getTier(),
            campaign.getReference(),
            campaign.getRosterSize(),
            campaign.getFirstWeekStart(),
            campaign.getLastWeekStart(),
            today,
            currentWeekIndex,
            base(days),
            forecast(campaign, weeks, days, today),
            weekResponses(weeks, days, currentWeekIndex),
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
            return new CampaignBaseResponse(
                0, 0, 0, 0, 0, 0, 0, 0,
                CampaignRuleset.COMPONENTS_PER_RESCUE, CampaignRuleset.FOOD_PER_RESCUE, GUARDIAN_LOSS_PERCENT
            );
        }

        CampaignDailySnapshot last = days.getLast();
        double previous = days.size() > 1 ? days.get(days.size() - 2).getPopulation().doubleValue() : 0;
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
            (int) Math.floor(Math.max(0, food - protectedFood) / CampaignRuleset.FOOD_PER_RESCUE),
            (int) Math.round(population - previous),
            CampaignRuleset.COMPONENTS_PER_RESCUE,
            CampaignRuleset.FOOD_PER_RESCUE,
            GUARDIAN_LOSS_PERCENT
        );
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
        Optional<CampaignWeek> current = weeks.stream()
            .filter(week -> week.getWeekStart().equals(weekStart))
            .filter(week -> !week.isSettled())
            .findFirst();

        if (current.isEmpty()) {
            return null;
        }

        CampaignWeek week = current.orElseThrow();
        CampaignDailySnapshot last = days.getLast();
        ExtractionEstimate estimate = ExtractionEstimate.of(
            week.getWoundedCount(),
            week.getChallengeRescued(),
            last.getFoodStock().doubleValue(),
            last.getComponentsStock().doubleValue(),
            last.getPopulation().doubleValue(),
            progressPercent(week) / (double) PERCENT
        );

        return new CampaignForecastResponse(
            week.getWeekIndex(),
            week.getWoundedCount(),
            estimate.challengeRescued(),
            estimate.extracted(),
            estimate.rescued(),
            week.getWoundedCount() - estimate.rescued(),
            estimate.limiter()
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
            weeks.stream().filter(CampaignWeek::isSettled).mapToInt(CampaignWeek::rescued).sum(),
            weeks.stream().filter(CampaignWeek::isSettled).mapToInt(CampaignWeek::getChallengeRescued).sum(),
            days.stream().mapToLong(CampaignDailySnapshot::getDamage).sum(),
            days.stream().mapToLong(CampaignDailySnapshot::getFoodGained).sum(),
            days.stream().mapToLong(CampaignDailySnapshot::getComponentsGained).sum(),
            (int) Math.round(lost)
        );
    }

    /**
     * Maps the stored weeks to what the map shows, each closed by the base as it stood that Sunday.
     *
     * @param weeks            stored weeks, week one first
     * @param days             the campaign's days, oldest first
     * @param currentWeekIndex one-based week in progress, {@code null} before the campaign starts
     * @return the week responses
     */
    private List<CampaignWeekResponse> weekResponses(
        List<CampaignWeek> weeks,
        List<CampaignDailySnapshot> days,
        Integer currentWeekIndex
    ) {
        List<CampaignWeekResponse> responses = new ArrayList<>(weeks.size());
        double previousPopulation = 0;
        // A guardian is revealed when its week is reached; the ones ahead stay a category.
        int revealedUpTo = currentWeekIndex == null ? 0 : currentWeekIndex;

        for (CampaignWeek week : weeks) {
            CampaignWeekBaseResponse base = weekBase(week, days, previousPopulation);
            responses.add(toWeekResponse(week, base, week.getWeekIndex() <= revealedUpTo));
            if (base != null) {
                previousPopulation = base.population();
            }
        }

        return responses;
    }

    /**
     * Reads the base at the close of one week from its replayed days.
     *
     * @param week               stored week
     * @param days               the campaign's days, oldest first
     * @param previousPopulation inhabitants at the previous week's close
     * @return the week's base, {@code null} before its first replayed day
     */
    private CampaignWeekBaseResponse weekBase(
        CampaignWeek week,
        List<CampaignDailySnapshot> days,
        double previousPopulation
    ) {
        List<CampaignDailySnapshot> weekDays = days.stream()
            .filter(day -> !day.getDay().isBefore(week.getWeekStart()) && !day.getDay().isAfter(week.settlementDay()))
            .toList();

        if (weekDays.isEmpty()) {
            return null;
        }

        CampaignDailySnapshot last = weekDays.getLast();
        double population = last.getPopulation().doubleValue();

        return new CampaignWeekBaseResponse(
            (int) Math.round(population),
            (int) Math.round(population - previousPopulation),
            (int) Math.round(last.getFoodStock().doubleValue()),
            (int) Math.round(last.getComponentsStock().doubleValue()),
            weekDays.stream().mapToInt(CampaignDailySnapshot::getFoodGained).sum(),
            weekDays.stream().mapToInt(CampaignDailySnapshot::getComponentsGained).sum()
        );
    }

    /**
     * Maps one stored week to what the map shows.
     *
     * @param week     stored week
     * @param base     the base at the week's close
     * @param revealed whether the week's guardian is named yet
     * @return the week response
     */
    private CampaignWeekResponse toWeekResponse(CampaignWeek week, CampaignWeekBaseResponse base, boolean revealed) {
        return new CampaignWeekResponse(
            week.getWeekIndex(),
            week.getWeekStart(),
            week.getPlanetName(),
            week.getCategory(),
            revealed ? week.getGuardian().getName() : null,
            revealed ? week.getGuardian().getDescription() : null,
            week.getGuardianHitPoints(),
            week.getDamageDealt(),
            progressPercent(week),
            week.isDefeated(),
            week.getDefeatedAt(),
            week.getDefeatedByPlayer() == null ? null : week.getDefeatedByPlayer().getId(),
            fatalBlow(week.getFinishingPlayerMatch()),
            week.getWoundedCount(),
            week.getChallengeRescued(),
            week.getExtractionRescued(),
            week.getFoodSpent(),
            week.getComponentsSpent(),
            week.getLimiter(),
            (int) Math.round(week.getBaseLoss().doubleValue()),
            week.isSettled(),
            base
        );
    }

    /**
     * Names the match that brought the guardian down, the way the mission report states it.
     *
     * @param playerMatch the finishing match, {@code null} while the guardian stands
     * @return the fatal blow, or {@code null}
     */
    private FatalBlowResponse fatalBlow(PlayerMatch playerMatch) {
        if (playerMatch == null) {
            return null;
        }
        ValorantMatch match = playerMatch.getMatch();
        boolean redTeam = "Red".equalsIgnoreCase(playerMatch.getTeamId());
        return new FatalBlowResponse(
            match.getMapName(),
            match.getGameMode(),
            playerMatch.getResult(),
            redTeam ? match.getRedScore() : match.getBlueScore(),
            redTeam ? match.getBlueScore() : match.getRedScore(),
            playerMatch.getAgentName()
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
            campaign.getId(),
            campaign.getNumber(),
            campaign.getTier(),
            campaign.getReference(),
            campaign.getRosterSize(),
            campaign.getFirstWeekStart(),
            campaign.getLastWeekStart(),
            campaign.getStoppedOn(),
            (int) weeks.stream().filter(CampaignWeek::isDefeated).count(),
            days.isEmpty() ? 0 : (int) Math.round(days.getLast().getPopulation().doubleValue()),
            weeks.stream().filter(CampaignWeek::isSettled).mapToInt(CampaignWeek::rescued).sum(),
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
