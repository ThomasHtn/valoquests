package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
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
import io.github.thomashtn.valoquests.campaign.repository.CampaignDailySnapshotRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerDayRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.match.entity.PlayerMatch;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.scoring.model.PlayerDayOutput;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies that a replay replaces everything a campaign holds, rather than adding to it.
 */
@ExtendWith(MockitoExtension.class)
class CampaignReplayWriterTest {

    /**
     * Instant the guardian fell.
     */
    private static final Instant FINISHING_INSTANT = Instant.parse("2026-09-09T20:15:00Z");

    @Mock
    private CampaignWeekRepository weekRepository;

    @Mock
    private CampaignDailySnapshotRepository snapshotRepository;

    @Mock
    private CampaignPlayerDayRepository playerDayRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private CampaignReplayWriter writer;

    private Campaign campaign;

    private CampaignWeek week;

    @BeforeEach
    void setUp() {
        campaign = CampaignFixtures.runningCampaign(1);
        week = CampaignFixtures.week(campaign, 1, 1_000, 50);
    }

    @Test
    @DisplayName("Empties the campaign before writing it again")
    void shouldDeleteBeforeWriting() {
        writer.write(campaign, List.of(week), inputs(GuardianFight.UNTOUCHED, List.of()), result(List.of()));

        InOrder order = inOrder(snapshotRepository, playerDayRepository);
        order.verify(snapshotRepository).deleteAllByCampaignId(1L);
        order.verify(playerDayRepository).deleteAllByCampaignId(1L);
        order.verify(snapshotRepository).flush();
        order.verify(playerDayRepository).flush();
        order.verify(snapshotRepository).saveAll(any());
    }

    @Test
    @DisplayName("Writes a week's fight and its Sunday from the replay")
    void shouldWriteTheFightAndTheSettlement() {
        Player finisher = CampaignFixtures.player(3, "Charlie");
        PlayerMatch finishingMatch = new PlayerMatch();
        when(entityManager.getReference(eq(Player.class), anyLong())).thenReturn(finisher);
        when(entityManager.getReference(eq(PlayerMatch.class), anyLong())).thenReturn(finishingMatch);

        GuardianFight fight = new GuardianFight(1_200, true, FINISHING_INSTANT, 3L, 30L);
        CampaignWeekSettlement settlement =
            new CampaignWeekSettlement(1, 12, 30, 360, 420, ExtractionLimiter.COMPONENTS, 4.5);

        writer.write(campaign, List.of(week), inputs(fight, List.of()), result(List.of(settlement)));

        assertThat(week.getDamageDealt()).isEqualTo(1_200);
        assertThat(week.isDefeated()).isTrue();
        assertThat(week.getDefeatedAt()).isEqualTo(FINISHING_INSTANT);
        assertThat(week.getDefeatedByPlayer()).isSameAs(finisher);
        assertThat(week.getFinishingPlayerMatch()).isSameAs(finishingMatch);
        assertThat(week.getChallengeRescued()).isEqualTo(12);
        assertThat(week.getExtractionRescued()).isEqualTo(30);
        assertThat(week.getFoodSpent()).isEqualTo(360);
        assertThat(week.getComponentsSpent()).isEqualTo(420);
        assertThat(week.getLimiter()).isEqualTo(ExtractionLimiter.COMPONENTS);
        assertThat(week.getBaseLoss()).isEqualByComparingTo("4.500");
        assertThat(week.isSettled()).isTrue();
    }

    @Test
    @DisplayName("Clears a week the replay no longer settles")
    void shouldClearAWeekWithoutASettlement() {
        week.setChallengeRescued(9);
        week.setExtractionRescued(20);
        week.setLimiter(ExtractionLimiter.FOOD);
        week.setBaseLoss(BigDecimal.TEN);
        week.setSettled(true);

        writer.write(campaign, List.of(week), inputs(GuardianFight.UNTOUCHED, List.of()), result(List.of()));

        assertThat(week.getChallengeRescued()).isZero();
        assertThat(week.getExtractionRescued()).isZero();
        assertThat(week.getLimiter()).isEqualTo(ExtractionLimiter.NONE);
        assertThat(week.getBaseLoss()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(week.isSettled()).isFalse();
        assertThat(week.getDefeatedByPlayer()).isNull();
        assertThat(week.getFinishingPlayerMatch()).isNull();
    }

    @Test
    @DisplayName("Stores each day and each operator day the replay computed")
    void shouldStoreEveryComputedRow() {
        Player operator = CampaignFixtures.player(1, "Alpha");
        when(entityManager.getReference(eq(Player.class), anyLong())).thenReturn(operator);

        CampaignDayState state = new CampaignDayState(
            campaign.getFirstWeekStart(), 2_800, 840, 1_960, 100, 0.8, 0, 0, 0, 839.2, 1_960, 100, 1
        );
        CampaignPlayerDayInput playerDay = new CampaignPlayerDayInput(
            1L,
            campaign.getFirstWeekStart(),
            new PlayerDayOutput(2_800, 840, 1_960, 4, 1, 3, 4)
        );

        writer.write(
            campaign,
            List.of(week),
            inputs(GuardianFight.UNTOUCHED, List.of(playerDay)),
            result(List.of(), List.of(state))
        );

        ArgumentCaptor<List<CampaignDailySnapshot>> snapshots = ArgumentCaptor.captor();
        verify(snapshotRepository).saveAll(snapshots.capture());
        assertThat(snapshots.getValue()).singleElement().satisfies(saved -> {
            assertThat(saved.getDay()).isEqualTo(campaign.getFirstWeekStart());
            assertThat(saved.getPopulation()).isEqualByComparingTo("100.000");
            assertThat(saved.getFoodStock()).isEqualByComparingTo("839.200");
            assertThat(saved.getPresenceCount()).isEqualTo(1);
        });

        ArgumentCaptor<List<CampaignPlayerDay>> playerDays = ArgumentCaptor.captor();
        verify(playerDayRepository).saveAll(playerDays.capture());
        assertThat(playerDays.getValue()).singleElement().satisfies(saved -> {
            assertThat(saved.getPlayer()).isSameAs(operator);
            assertThat(saved.getDamage()).isEqualTo(2_800);
            assertThat(saved.getReducedMatchCount()).isEqualTo(1);
            assertThat(saved.getStreakBonusPercent()).isEqualTo(4);
        });
    }

    /**
     * Builds the replay inputs with one week's fight.
     *
     * @param fight      fight of week one
     * @param playerDays operator days to store
     * @return the inputs
     */
    private CampaignReplayInputs inputs(GuardianFight fight, List<CampaignPlayerDayInput> playerDays) {
        return new CampaignReplayInputs(List.of(), List.of(), Map.of(1, fight), Map.of(), playerDays);
    }

    /**
     * Builds a result holding settlements only.
     *
     * @param settlements settlements the replay produced
     * @return the result
     */
    private CampaignReplayResult result(List<CampaignWeekSettlement> settlements) {
        return result(settlements, List.of());
    }

    /**
     * Builds a result holding days and settlements.
     *
     * @param settlements settlements the replay produced
     * @param days        days the replay produced
     * @return the result
     */
    private CampaignReplayResult result(List<CampaignWeekSettlement> settlements, List<CampaignDayState> days) {
        return new CampaignReplayResult(days, settlements);
    }
}
