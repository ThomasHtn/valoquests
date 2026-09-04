package io.github.thomashtn.valoquests.campaign.service;

import io.github.thomashtn.valoquests.campaign.CampaignRuleset;
import io.github.thomashtn.valoquests.campaign.model.CampaignDayInput;
import io.github.thomashtn.valoquests.campaign.model.CampaignDayState;
import io.github.thomashtn.valoquests.campaign.model.CampaignReplayResult;
import io.github.thomashtn.valoquests.campaign.model.CampaignWeekInput;
import io.github.thomashtn.valoquests.campaign.model.CampaignWeekSettlement;
import io.github.thomashtn.valoquests.campaign.model.ExtractionEstimate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Plays a campaign forward, day by day, from nothing but its inputs.
 *
 * <p>Pure on purpose: no repository, no clock, no entity. Handed the same days it returns the same
 * base, which is what lets the replay run after every synchronization, every night and on every
 * admin click without any of them being able to disagree.
 *
 * <p>The order inside a day is the order the rules are written in: the base grows, the stocks fill,
 * the base eats, and on a Sunday the ship leaves. The rescued arrive after the guardian has struck,
 * because they were not there to be struck at.
 */
@Component
public class CampaignReplayEngine {

    /**
     * Replays a campaign from its first day to its last.
     *
     * @param days  every day of the campaign, oldest first, days nobody played included
     * @param weeks the weeks whose Sunday falls inside those days
     * @return the days computed and the weeks settled
     */
    public CampaignReplayResult replay(List<CampaignDayInput> days, List<CampaignWeekInput> weeks) {
        Map<LocalDate, CampaignWeekInput> bySettlementDay = new HashMap<>();
        weeks.forEach(week -> bySettlementDay.put(week.settlementDay(), week));

        List<CampaignDayState> states = new ArrayList<>(days.size());
        List<CampaignWeekSettlement> settlements = new ArrayList<>(weeks.size());
        Base base = new Base();

        for (CampaignDayInput day : days) {
            base.grow(day);
            double eaten = base.eat();
            double famineLoss = base.famineLoss;

            CampaignWeekInput week = bySettlementDay.get(day.day());
            CampaignWeekSettlement settlement = week == null ? null : settle(week, base);
            if (settlement != null) {
                settlements.add(settlement);
            }

            states.add(new CampaignDayState(
                day.day(),
                day.damage(),
                day.food(),
                day.components(),
                base.growth,
                eaten,
                famineLoss,
                settlement == null ? 0 : settlement.baseLoss(),
                settlement == null ? 0 : settlement.challengeRescued() + settlement.extractionRescued(),
                base.food,
                base.components,
                base.population,
                day.presenceCount()
            ));
        }

        return new CampaignReplayResult(states, settlements);
    }

    /**
     * Settles one week's Sunday against the base as it stands after that evening's meal.
     *
     * @param week week to settle
     * @param base base to spend from and to grow, mutated in place
     * @return what the rescue cost and brought back
     */
    private CampaignWeekSettlement settle(CampaignWeekInput week, Base base) {
        double progress = week.progress();
        ExtractionEstimate estimate = ExtractionEstimate.of(
            week.woundedCount(),
            week.challengeRescued(),
            base.food,
            base.components,
            base.population,
            progress
        );

        int foodSpent = estimate.extracted() * CampaignRuleset.FOOD_PER_RESCUE;
        int componentsSpent = estimate.extracted() * CampaignRuleset.COMPONENTS_PER_RESCUE;
        base.spend(foodSpent, componentsSpent);

        double baseLoss = base.strike(progress);
        base.settle(estimate.rescued());

        return new CampaignWeekSettlement(
            week.weekIndex(),
            estimate.challengeRescued(),
            estimate.extracted(),
            foodSpent,
            componentsSpent,
            estimate.limiter(),
            baseLoss
        );
    }

    /**
     * The base as it stands between two steps of a day, mutated as the day is played.
     *
     * <p>Local to one replay and never shared: the engine itself stays stateless, which is what
     * makes it safe to hold as a singleton bean.
     */
    private static final class Base {

        /**
         * Food in reserve.
         */
        private double food;

        /**
         * Components in reserve.
         */
        private double components;

        /**
         * Inhabitants.
         */
        private double population;

        /**
         * Inhabitants the current day's damage added.
         */
        private double growth;

        /**
         * Inhabitants the current day's famine killed.
         */
        private double famineLoss;

        /**
         * Adds one day's production to the base.
         *
         * @param day day being played
         */
        private void grow(CampaignDayInput day) {
            growth = day.damage() / CampaignRuleset.DAMAGE_PER_INHABITANT;
            population += growth;
            food += day.food();
            components += day.components();
            famineLoss = 0;
        }

        /**
         * Feeds the base for one evening, killing a share of the unfed when the larder runs out.
         *
         * @return the food actually eaten
         */
        private double eat() {
            double needed = population * CampaignRuleset.FOOD_PER_INHABITANT_PER_DAY;

            if (food >= needed) {
                food -= needed;
                return needed;
            }

            double eaten = food;
            double fed = food / CampaignRuleset.FOOD_PER_INHABITANT_PER_DAY;
            famineLoss = Math.max(0, population - fed) * CampaignRuleset.FAMINE_LOSS_RATE;
            population -= famineLoss;
            food = 0;

            return eaten;
        }

        /**
         * Spends what the extraction cost.
         *
         * @param spentFood       food spent
         * @param spentComponents components spent
         */
        private void spend(int spentFood, int spentComponents) {
            food -= spentFood;
            components -= spentComponents;
        }

        /**
         * Lets a guardian that is still standing strike the base.
         *
         * @param progress share of the guardian's hit points the squad removed
         * @return the inhabitants it killed
         */
        private double strike(double progress) {
            double remaining = 1 - progress;
            double loss = population * remaining * remaining * CampaignRuleset.GUARDIAN_LOSS_RATE;
            population -= loss;

            return loss;
        }

        /**
         * Settles the rescued into the base, after the guardian has struck.
         *
         * @param rescued wounded brought home
         */
        private void settle(int rescued) {
            population += rescued;
        }
    }
}
