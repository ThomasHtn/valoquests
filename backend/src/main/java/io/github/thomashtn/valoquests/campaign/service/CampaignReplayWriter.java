package io.github.thomashtn.valoquests.campaign.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignDailySnapshot;
import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayerDay;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignDayState;
import io.github.thomashtn.valoquests.campaign.model.CampaignPlayerDayInput;
import io.github.thomashtn.valoquests.campaign.model.CampaignReplayInputs;
import io.github.thomashtn.valoquests.campaign.model.CampaignReplayResult;
import io.github.thomashtn.valoquests.campaign.model.CampaignWeekSettlement;
import io.github.thomashtn.valoquests.campaign.model.ExtractionLimiter;
import io.github.thomashtn.valoquests.campaign.model.GuardianFight;
import io.github.thomashtn.valoquests.campaign.model.WeekChallengeYield;
import io.github.thomashtn.valoquests.campaign.repository.CampaignDailySnapshotRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerDayRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.player.entity.Player;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores what one replay produced, replacing everything the campaign held.
 *
 * <p>Deleted and written again rather than updated in place. A day the roster no longer has any
 * match on must disappear, not keep the figures of the run before, and the whole promise of the
 * replay is that its rows depend on nothing but the inputs it just read.
 */
@Service
public class CampaignReplayWriter {

    /**
     * Decimals the {@code NUMERIC} columns keep.
     *
     * <p>Display precision only: the replay always restarts from an empty base and never reads a
     * stored value back, so rounding here can never compound from one day into the next.
     */
    private static final int STORED_SCALE = 3;

    /**
     * Repository holding the campaign's weeks.
     */
    private final CampaignWeekRepository weekRepository;

    /**
     * Repository holding the campaign's days.
     */
    private final CampaignDailySnapshotRepository snapshotRepository;

    /**
     * Repository holding the campaign's per-operator days.
     */
    private final CampaignPlayerDayRepository playerDayRepository;

    /**
     * Entity manager used to reference players and matches without loading them.
     */
    private final EntityManager entityManager;

    /**
     * Creates the replay writer.
     *
     * @param weekRepository      campaign week repository
     * @param snapshotRepository  campaign daily snapshot repository
     * @param playerDayRepository campaign player day repository
     * @param entityManager       entity manager
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = """
            The EntityManager is a Spring-managed shared proxy, not a value this service owns:
            copying it would break the thread-bound persistence context the writes participate in.
            """
    )
    public CampaignReplayWriter(
        CampaignWeekRepository weekRepository,
        CampaignDailySnapshotRepository snapshotRepository,
        CampaignPlayerDayRepository playerDayRepository,
        EntityManager entityManager
    ) {
        this.weekRepository = weekRepository;
        this.snapshotRepository = snapshotRepository;
        this.playerDayRepository = playerDayRepository;
        this.entityManager = entityManager;
    }

    /**
     * Replaces every row one campaign owns with what the replay just computed.
     *
     * @param campaign campaign being replayed
     * @param weeks    the campaign's weeks, week one first
     * @param inputs   what the replay read
     * @param result   what the engine produced
     */
    @Transactional
    public void write(
        Campaign campaign,
        List<CampaignWeek> weeks,
        CampaignReplayInputs inputs,
        CampaignReplayResult result
    ) {
        snapshotRepository.deleteAllByCampaignId(campaign.getId());
        playerDayRepository.deleteAllByCampaignId(campaign.getId());
        snapshotRepository.flush();
        playerDayRepository.flush();

        snapshotRepository.saveAll(result.days().stream().map(day -> toSnapshot(campaign, day)).toList());
        playerDayRepository.saveAll(inputs.playerDays().stream().map(day -> toPlayerDay(campaign, day)).toList());
        weekRepository.saveAll(settle(weeks, inputs, result));
    }

    /**
     * Rewrites every week's outcome from the fights and settlements of this replay.
     *
     * @param weeks  the campaign's weeks
     * @param inputs what the replay read
     * @param result what the engine produced
     * @return the weeks, ready to persist
     */
    private List<CampaignWeek> settle(
        List<CampaignWeek> weeks,
        CampaignReplayInputs inputs,
        CampaignReplayResult result
    ) {
        Map<Integer, CampaignWeekSettlement> settlements = new HashMap<>();
        result.settlements().forEach(settlement -> settlements.put(settlement.weekIndex(), settlement));

        for (CampaignWeek week : weeks) {
            applyFight(week, inputs.fights().getOrDefault(week.getWeekIndex(), GuardianFight.UNTOUCHED));
            applySettlement(
                week,
                settlements.get(week.getWeekIndex()),
                inputs.yields().getOrDefault(week.getWeekIndex(), WeekChallengeYield.NONE)
            );
        }

        return weeks;
    }

    /**
     * Writes one week's guardian fight, clearing it when the week has not been played.
     *
     * @param week  week to write
     * @param fight fight of that week
     */
    private void applyFight(CampaignWeek week, GuardianFight fight) {
        week.setDamageDealt(fight.damageDealt());
        week.setDefeated(fight.defeated());
        week.setDefeatedAt(fight.defeatedAt());
        week.setDefeatedByPlayer(reference(Player.class, fight.playerId()));
        week.setFinishingPlayerMatch(reference(PlayerMatch.class, fight.playerMatchId()));
    }

    /**
     * Writes one week's Sunday, clearing it when that Sunday has not been reached.
     *
     * <p>A week still ahead of its Sunday keeps what its challenges have already brought home: those
     * wounded are acquired whatever the guardian does, and the forecast of the week in progress
     * reads them from here.
     *
     * @param week       week to write
     * @param settlement settlement of that week, {@code null} while its Sunday is still ahead
     * @param yield      what the week's challenges have brought home so far
     */
    private void applySettlement(CampaignWeek week, CampaignWeekSettlement settlement, WeekChallengeYield yield) {
        if (settlement == null) {
            week.setChallengeRescued(Math.min(yield.survivors(), week.getWoundedCount()));
            week.setExtractionRescued(0);
            week.setFoodSpent(0);
            week.setComponentsSpent(0);
            week.setLimiter(ExtractionLimiter.NONE);
            week.setBaseLoss(BigDecimal.ZERO);
            week.setSettled(false);

            return;
        }

        week.setChallengeRescued(settlement.challengeRescued());
        week.setExtractionRescued(settlement.extractionRescued());
        week.setFoodSpent(settlement.foodSpent());
        week.setComponentsSpent(settlement.componentsSpent());
        week.setLimiter(settlement.limiter());
        week.setBaseLoss(stored(settlement.baseLoss()));
        week.setSettled(true);
    }

    /**
     * Maps one computed day to the row that stores it.
     *
     * @param campaign campaign the day belongs to
     * @param state    computed day
     * @return the snapshot, ready to persist
     */
    private CampaignDailySnapshot toSnapshot(Campaign campaign, CampaignDayState state) {
        CampaignDailySnapshot snapshot = new CampaignDailySnapshot();
        snapshot.setCampaign(campaign);
        snapshot.setDay(state.day());
        snapshot.setDamage(state.damage());
        snapshot.setFoodGained(state.foodGained());
        snapshot.setComponentsGained(state.componentsGained());
        snapshot.setGrowth(stored(state.growth()));
        snapshot.setEaten(stored(state.eaten()));
        snapshot.setFamineLoss(stored(state.famineLoss()));
        snapshot.setGuardianLoss(stored(state.guardianLoss()));
        snapshot.setArrivals(state.arrivals());
        snapshot.setFoodStock(stored(state.foodStock()));
        snapshot.setComponentsStock(stored(state.componentsStock()));
        snapshot.setPopulation(stored(state.population()));
        snapshot.setPresenceCount(state.presenceCount());

        return snapshot;
    }

    /**
     * Maps one operator's day to the row that stores it.
     *
     * @param campaign campaign the day belongs to
     * @param input    the operator's day
     * @return the row, ready to persist
     */
    private CampaignPlayerDay toPlayerDay(Campaign campaign, CampaignPlayerDayInput input) {
        CampaignPlayerDay row = new CampaignPlayerDay();
        row.setCampaign(campaign);
        row.setPlayer(entityManager.getReference(Player.class, input.playerId()));
        row.setDay(input.day());
        row.setDamage(input.output().damage());
        row.setFood(input.output().food());
        row.setComponents(input.output().components());
        row.setMatchCount(input.output().matchCount());
        row.setReducedMatchCount(input.output().reducedMatchCount());
        row.setStreakDays(input.output().streakDays());
        row.setStreakBonusPercent(input.output().streakBonusPercent());

        return row;
    }

    /**
     * Returns a lazy reference to one entity, or {@code null} for an absent identifier.
     *
     * @param type       entity type
     * @param identifier entity identifier, may be {@code null}
     * @param <T>        entity type
     * @return the reference, or {@code null}
     */
    private <T> T reference(Class<T> type, Long identifier) {
        return identifier == null ? null : entityManager.getReference(type, identifier);
    }

    /**
     * Rounds one computed value to what the column keeps.
     *
     * @param value computed value
     * @return the value at the stored scale
     */
    private static BigDecimal stored(double value) {
        return BigDecimal.valueOf(value).setScale(STORED_SCALE, RoundingMode.HALF_UP);
    }
}
