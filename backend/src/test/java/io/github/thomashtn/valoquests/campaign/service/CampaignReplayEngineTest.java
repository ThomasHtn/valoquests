package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.github.thomashtn.valoquests.campaign.model.CampaignDayInput;
import io.github.thomashtn.valoquests.campaign.model.CampaignDayState;
import io.github.thomashtn.valoquests.campaign.model.CampaignReplayResult;
import io.github.thomashtn.valoquests.campaign.model.CampaignWeekInput;
import io.github.thomashtn.valoquests.campaign.model.CampaignWeekSettlement;
import io.github.thomashtn.valoquests.campaign.model.ExtractionLimiter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the campaign engine against the rules written in {@code docs/GAMEPLAY.md}.
 *
 * <p>Every scenario is built on round numbers so an expectation can be read as arithmetic rather
 * than trusted: 2 800 damage is exactly a hundred inhabitants, and a hundred inhabitants eat exactly
 * 0.8 food an evening.
 */
class CampaignReplayEngineTest {

    /**
     * Monday the scenarios start on.
     */
    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 7);

    /**
     * Sunday those weeks settle on.
     */
    private static final LocalDate SUNDAY = MONDAY.plusDays(6);

    /**
     * Damage worth exactly a hundred inhabitants.
     */
    private static final int DAMAGE_FOR_HUNDRED = 2_800;

    /**
     * Tolerance on figures the engine keeps as doubles.
     */
    private static final double TOLERANCE = 0.0001;

    /**
     * Engine under test, on the real constants.
     */
    private final CampaignReplayEngine engine = new CampaignReplayEngine();

    @Test
    @DisplayName("Turns a day's damage into inhabitants and its production into stocks")
    void shouldGrowTheBaseAndFillTheStocks() {
        CampaignReplayResult result = engine.replay(
            List.of(new CampaignDayInput(MONDAY, DAMAGE_FOR_HUNDRED, 840, 1_960, 2)),
            List.of()
        );

        CampaignDayState day = result.days().getFirst();
        assertThat(day.growth()).isCloseTo(100, within(TOLERANCE));
        assertThat(day.population()).isCloseTo(100, within(TOLERANCE));
        assertThat(day.eaten()).isCloseTo(0.8, within(TOLERANCE));
        assertThat(day.foodStock()).isCloseTo(839.2, within(TOLERANCE));
        assertThat(day.componentsStock()).isCloseTo(1_960, within(TOLERANCE));
        assertThat(day.famineLoss()).isZero();
        assertThat(day.presenceCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Kills a twentieth of the unfed on an evening the larder is empty")
    void shouldStarveOnlyWhenTheLarderIsEmpty() {
        CampaignReplayResult result = engine.replay(
            List.of(new CampaignDayInput(MONDAY, DAMAGE_FOR_HUNDRED, 0, 0, 1)),
            List.of()
        );

        CampaignDayState day = result.days().getFirst();
        assertThat(day.famineLoss()).isCloseTo(5, within(TOLERANCE));
        assertThat(day.population()).isCloseTo(95, within(TOLERANCE));
        assertThat(day.eaten()).isZero();
        assertThat(day.foodStock()).isZero();
    }

    @Test
    @DisplayName("Never starves a base that has food left in reserve")
    void shouldNotStarveWithFoodInReserve() {
        List<CampaignDayInput> days = new ArrayList<>();
        days.add(new CampaignDayInput(MONDAY, DAMAGE_FOR_HUNDRED, 100, 0, 1));
        for (int index = 1; index < 7; index++) {
            days.add(new CampaignDayInput(MONDAY.plusDays(index), 0, 0, 0, 0));
        }

        CampaignReplayResult result = engine.replay(days, List.of());

        assertThat(result.days()).allSatisfy(day -> assertThat(day.famineLoss()).isZero());
        assertThat(result.population()).isCloseTo(100, within(TOLERANCE));
    }

    @Test
    @DisplayName("Brings the whole group home when nothing binds")
    void shouldRescueEveryoneWhenNothingBinds() {
        CampaignReplayResult result = engine.replay(week(2_000, 2_000), List.of(defeatedWeek(10)));

        CampaignWeekSettlement settlement = result.settlements().getFirst();
        assertThat(settlement.challengeRescued()).isEqualTo(10);
        assertThat(settlement.extractionRescued()).isEqualTo(40);
        assertThat(settlement.foodSpent()).isEqualTo(480);
        assertThat(settlement.componentsSpent()).isEqualTo(560);
        assertThat(settlement.limiter()).isEqualTo(ExtractionLimiter.NONE);
        assertThat(settlement.baseLoss()).isZero();
        assertThat(result.population()).isCloseTo(150, within(TOLERANCE));
        assertThat(result.days().getLast().arrivals()).isEqualTo(50);
    }

    @Test
    @DisplayName("Reports the components as the binding constraint when they run out first")
    void shouldReportComponentsAsTheLimiter() {
        CampaignReplayResult result = engine.replay(week(2_000, 300), List.of(defeatedWeek(0)));

        CampaignWeekSettlement settlement = result.settlements().getFirst();
        assertThat(settlement.extractionRescued()).isEqualTo(21);
        assertThat(settlement.limiter()).isEqualTo(ExtractionLimiter.COMPONENTS);
    }

    @Test
    @DisplayName("Reports the food as the binding constraint when it runs out first")
    void shouldReportFoodAsTheLimiter() {
        CampaignReplayResult result = engine.replay(week(200, 2_000), List.of(defeatedWeek(0)));

        CampaignWeekSettlement settlement = result.settlements().getFirst();
        assertThat(settlement.extractionRescued()).isEqualTo(16);
        assertThat(settlement.limiter()).isEqualTo(ExtractionLimiter.FOOD);
    }

    @Test
    @DisplayName("Leaves the seven next evenings of food untouched")
    void shouldProtectSevenEveningsOfFood() {
        // 100 inhabitants eat 0.8 an evening, so 5.6 of the 200 in store may never pay a berth:
        // 194.4 buys sixteen, where the whole 200 would have bought sixteen as well but left the
        // base with nothing to eat on Monday.
        CampaignReplayResult result = engine.replay(week(200, 2_000), List.of(defeatedWeek(0)));

        assertThat(result.days().getLast().foodStock()).isCloseTo(7.2, within(TOLERANCE));
    }

    @Test
    @DisplayName("Extracts in proportion to the progress and lets a standing guardian strike")
    void shouldScaleExtractionAndLossesOnProgress() {
        CampaignReplayResult result = engine.replay(week(2_000, 2_000), List.of(
            new CampaignWeekInput(1, SUNDAY, 1_000, 50, 700, false, 0)
        ));

        CampaignWeekSettlement settlement = result.settlements().getFirst();
        assertThat(settlement.extractionRescued()).isEqualTo(35);
        assertThat(settlement.limiter()).isEqualTo(ExtractionLimiter.GROUP);
        assertThat(settlement.baseLoss()).isCloseTo(100 * 0.09 * 0.35, within(TOLERANCE));
        assertThat(result.population()).isCloseTo(100 - 3.15 + 35, within(TOLERANCE));
    }

    @Test
    @DisplayName("Costs almost nothing to miss a guardian by a hair")
    void shouldBarelyPunishAGuardianMissedByAHair() {
        CampaignReplayResult result = engine.replay(week(2_000, 2_000), List.of(
            new CampaignWeekInput(1, SUNDAY, 1_000, 50, 990, false, 0)
        ));

        assertThat(result.settlements().getFirst().baseLoss()).isCloseTo(0.0035, within(TOLERANCE));
    }

    @Test
    @DisplayName("Caps the challenge rescues at the group, never above it")
    void shouldCapChallengeRescuesAtTheGroup() {
        CampaignReplayResult result = engine.replay(week(2_000, 2_000), List.of(defeatedWeek(80)));

        CampaignWeekSettlement settlement = result.settlements().getFirst();
        assertThat(settlement.challengeRescued()).isEqualTo(50);
        assertThat(settlement.extractionRescued()).isZero();
        assertThat(settlement.foodSpent()).isZero();
        assertThat(settlement.componentsSpent()).isZero();
        assertThat(settlement.limiter()).isEqualTo(ExtractionLimiter.NONE);
    }

    @Test
    @DisplayName("Settles nothing on a week whose Sunday the replay has not reached")
    void shouldSettleNothingBeforeSunday() {
        CampaignReplayResult result = engine.replay(
            List.of(new CampaignDayInput(MONDAY, DAMAGE_FOR_HUNDRED, 100, 100, 1)),
            List.of()
        );

        assertThat(result.settlements()).isEmpty();
        assertThat(result.days().getFirst().arrivals()).isZero();
        assertThat(result.days().getFirst().guardianLoss()).isZero();
    }

    @Test
    @DisplayName("Reports nothing at all before the campaign's first day")
    void shouldReportNothingWithoutADay() {
        CampaignReplayResult result = engine.replay(List.of(), List.of());

        assertThat(result.days()).isEmpty();
        assertThat(result.population()).isZero();
    }

    /**
     * Builds a week whose only production lands on its Sunday.
     *
     * @param food       food produced that Sunday
     * @param components components produced that Sunday
     * @return the seven days of the week
     */
    private List<CampaignDayInput> week(int food, int components) {
        List<CampaignDayInput> days = new ArrayList<>(7);

        for (int index = 0; index < 6; index++) {
            days.add(new CampaignDayInput(MONDAY.plusDays(index), 0, 0, 0, 0));
        }

        days.add(new CampaignDayInput(SUNDAY, DAMAGE_FOR_HUNDRED, food, components, 1));

        return days;
    }

    /**
     * Builds a week whose guardian fell, with fifty wounded stranded on the planet.
     *
     * @param challengeRescued wounded the week's challenges brought back
     * @return the week's settlement input
     */
    private CampaignWeekInput defeatedWeek(int challengeRescued) {
        return new CampaignWeekInput(1, SUNDAY, 1_000, 50, 1_000, true, challengeRescued);
    }
}
