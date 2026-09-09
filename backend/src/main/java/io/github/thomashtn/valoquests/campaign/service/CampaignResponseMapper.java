package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.CampaignRuleset;
import io.github.thomashtn.valoquests.campaign.dto.CampaignBaseResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignTotalsResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignWeekBaseResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignWeekResponse;
import io.github.thomashtn.valoquests.campaign.dto.FatalBlowResponse;
import io.github.thomashtn.valoquests.campaign.entity.CampaignDailySnapshot;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
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
    static final int PERCENT = 100;

    /**
     * Share of the base lost on a failed breakthrough, as an integer percentage.
     */
    static final int GUARDIAN_LOSS_PERCENT = (int) Math.round(CampaignRuleset.GUARDIAN_LOSS_RATE * PERCENT);

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
            CampaignWeekBaseResponse base = weekBase(week, days, previousPopulation);
            responses.add(week(week, base, week.getWeekIndex() <= revealedUpTo));
            if (base != null) {
                previousPopulation = base.population();
            }
        }

        return responses;
    }

    /**
     * Share of the guardian's hit points taken, capped at a full breakthrough.
     *
     * @param week the week
     * @return the percentage
     */
    static int progressPercent(CampaignWeek week) {
        if (week.isDefeated() || week.getGuardianHitPoints() <= 0) {
            return week.isDefeated() ? PERCENT : 0;
        }

        return Math.min(PERCENT, week.getDamageDealt() * PERCENT / week.getGuardianHitPoints());
    }

    /**
     * Population at the end of each settled week, in week order.
     *
     * @param weeks the campaign's weeks
     * @param days  replayed days, oldest first
     * @return one figure per settled week
     */
    static List<Integer> weeklyPopulation(List<CampaignWeek> weeks, List<CampaignDailySnapshot> days) {
        return weeks.stream()
            .filter(CampaignWeek::isSettled)
            .map(week -> days.stream()
                .filter(day -> day.getDay().equals(week.settlementDay()))
                .findFirst()
                .map(day -> (int) Math.round(day.getPopulation().doubleValue()))
                .orElse(0))
            .toList();
    }

    private static CampaignWeekBaseResponse weekBase(
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

    private static CampaignWeekResponse week(CampaignWeek week, CampaignWeekBaseResponse base, boolean revealed) {
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

    private static FatalBlowResponse fatalBlow(PlayerMatch playerMatch) {
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
}
