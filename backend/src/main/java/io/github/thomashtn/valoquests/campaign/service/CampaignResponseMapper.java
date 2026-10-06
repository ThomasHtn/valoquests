package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.CampaignRuleset;
import io.github.thomashtn.valoquests.campaign.dto.CampaignBaseResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignForecastResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignHistoryResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignTotalsResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignWeekBaseResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignWeekResponse;
import io.github.thomashtn.valoquests.campaign.dto.FatalBlowResponse;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignDailySnapshot;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.model.ExtractionEstimate;
import io.github.thomashtn.valoquests.campaign.model.RescueCapacity;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.match.entity.ValorantMatch;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns replayed campaign rows (weeks and daily snapshots) into API responses.
 *
 * <p>Pure mapping: no repository, no clock. The query service loads the rows and decides what is
 * revealed; this class only reads figures off them.</p>
 */
final class CampaignResponseMapper {

    /**
     * Scale of the percentages exposed by the API.
     */
    private static final int PERCENT = 100;

    /**
     * Margin added before rounding a share down to whole percents.
     */
    private static final double ROUNDING_TOLERANCE = 1e-9;

    /**
     * Share of the base lost on a failed breakthrough, as an integer percentage.
     */
    private static final int GUARDIAN_LOSS_PERCENT = (int) Math.round(CampaignRuleset.GUARDIAN_LOSS_RATE * PERCENT);

    /**
     * Not instantiable: static helpers only.
     */
    private CampaignResponseMapper() {
    }

    /**
     * Reads the base as it stands after the last replayed day.
     *
     * @param days replayed days, oldest first
     * @return the base, empty when no day has been replayed
     */
    static CampaignBaseResponse base(List<CampaignDailySnapshot> days) {
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
        RescueCapacity capacity = RescueCapacity.of(food, components, population);

        return new CampaignBaseResponse(
            (int) Math.round(population),
            (int) Math.round(food),
            (int) Math.round(components),
            (int) Math.round(CampaignRuleset.dailyUpkeep(population)),
            (int) Math.round(capacity.protectedFood()),
            capacity.byComponents(),
            capacity.byFood(),
            (int) Math.round(population - previous),
            CampaignRuleset.COMPONENTS_PER_RESCUE,
            CampaignRuleset.FOOD_PER_RESCUE,
            GUARDIAN_LOSS_PERCENT
        );
    }

    /**
     * Forecasts a week's Sunday from the base as it stands after the last replayed day.
     *
     * @param week week in progress, not settled yet
     * @param last last replayed day
     * @return the forecast
     */
    static CampaignForecastResponse forecast(CampaignWeek week, CampaignDailySnapshot last) {
        ExtractionEstimate estimate = ExtractionEstimate.of(
            week.getWoundedCount(),
            week.getChallengeRescued(),
            last.getFoodStock().doubleValue(),
            last.getComponentsStock().doubleValue(),
            last.getPopulation().doubleValue(),
            week.progress()
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
     * Sums the campaign so far.
     *
     * @param weeks the campaign's weeks
     * @param days  replayed days, oldest first
     * @return the totals
     */
    static CampaignTotalsResponse totals(List<CampaignWeek> weeks, List<CampaignDailySnapshot> days) {
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
     * Maps one closed campaign to its history row.
     *
     * @param campaign closed campaign
     * @param weeks    its weeks
     * @param days     its replayed days, oldest first
     * @return the history row
     */
    static CampaignHistoryResponse history(
        Campaign campaign,
        List<CampaignWeek> weeks,
        List<CampaignDailySnapshot> days
    ) {
        CampaignTotalsResponse totals = totals(weeks, days);

        return new CampaignHistoryResponse(
            campaign.getId(),
            campaign.getNumber(),
            campaign.getDifficulty(),
            campaign.reference(),
            campaign.getRosterSize(),
            campaign.getFirstWeekStart(),
            campaign.getLastWeekStart(),
            campaign.getStoppedOn(),
            totals.guardiansDefeated(),
            base(days).population(),
            totals.rescued(),
            weeklyPopulation(weeks, days)
        );
    }

    /**
     * Maps every week, revealing the guardian of the weeks already reached.
     *
     * @param weeks            the campaign's weeks, in order
     * @param days             replayed days, oldest first
     * @param currentWeekIndex index of the week being played, {@code null} before the campaign starts
     * @return one response per week
     */
    static List<CampaignWeekResponse> weeks(
        List<CampaignWeek> weeks,
        List<CampaignDailySnapshot> days,
        Integer currentWeekIndex
    ) {
        List<CampaignWeekResponse> responses = new ArrayList<>(weeks.size());
        double previousPopulation = 0;
        // A guardian is revealed when its week is reached; the ones ahead stay a category.
        int revealedUpTo = currentWeekIndex == null ? 0 : currentWeekIndex;

        for (CampaignWeek week : weeks) {
            List<CampaignDailySnapshot> weekDays = days.stream().filter(day -> week.contains(day.getDay())).toList();
            CampaignWeekBaseResponse base = weekBase(weekDays, previousPopulation);
            List<Integer> dailyDamage = weekDays.stream().map(CampaignDailySnapshot::getDamage).toList();
            responses.add(week(week, base, dailyDamage, week.getWeekIndex() <= revealedUpTo));
            if (base != null) {
                previousPopulation = base.population();
            }
        }

        return responses;
    }

    /**
     * Share of the guardian's hit points taken, in whole percents rounded down.
     *
     * @param week the week
     * @return the percentage
     */
    private static int progressPercent(CampaignWeek week) {
        // The tolerance absorbs binary rounding, so 29 damage out of 100 reads 29 and not 28.
        return (int) Math.floor(week.progress() * PERCENT + ROUNDING_TOLERANCE);
    }

    /**
     * Population at the end of each settled week, in week order.
     *
     * @param weeks the campaign's weeks
     * @param days  replayed days, oldest first
     * @return one figure per settled week
     */
    private static List<Integer> weeklyPopulation(List<CampaignWeek> weeks, List<CampaignDailySnapshot> days) {
        return weeks.stream()
            .filter(CampaignWeek::isSettled)
            .map(week -> days.stream()
                .filter(day -> day.getDay().equals(week.settlementDay()))
                .findFirst()
                .map(day -> (int) Math.round(day.getPopulation().doubleValue()))
                .orElse(0))
            .toList();
    }

    /**
     * The base at the end of one week, or {@code null} when none of its days were replayed.
     */
    private static CampaignWeekBaseResponse weekBase(List<CampaignDailySnapshot> weekDays, double previousPopulation) {
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
     * Maps one week, hiding the guardian while it is not revealed.
     */
    private static CampaignWeekResponse week(
        CampaignWeek week,
        CampaignWeekBaseResponse base,
        List<Integer> dailyDamage,
        boolean revealed
    ) {
        return new CampaignWeekResponse(
            week.getWeekIndex(),
            week.getWeekStart(),
            week.getPlanetName(),
            week.getCategory(),
            revealed ? week.getGuardian().getName() : null,
            revealed ? week.getGuardian().getDescription() : null,
            week.getGuardianHitPoints(),
            week.getDamageDealt(),
            dailyDamage,
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
     * The match that finished the guardian, seen from the player's side, or {@code null}.
     */
    private static FatalBlowResponse fatalBlow(PlayerMatch playerMatch) {
        if (playerMatch == null) {
            return null;
        }
        ValorantMatch match = playerMatch.getMatch();
        return new FatalBlowResponse(
            match.getMapName(),
            match.getGameMode(),
            playerMatch.getResult(),
            playerMatch.allyScore(),
            playerMatch.enemyScore(),
            playerMatch.getAgentName()
        );
    }
}
