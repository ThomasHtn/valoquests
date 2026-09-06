package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignDayInput;
import io.github.thomashtn.valoquests.campaign.model.CampaignReplayInputs;
import io.github.thomashtn.valoquests.campaign.model.GuardianFight;
import io.github.thomashtn.valoquests.campaign.model.WeekChallengeYield;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.scoring.model.DailyOutput;
import io.github.thomashtn.valoquests.scoring.model.PlayerDayOutput;
import io.github.thomashtn.valoquests.scoring.model.ValuedMatch;
import io.github.thomashtn.valoquests.scoring.service.DailyOutputReader;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies that a replay reads the frozen roster and nothing else, and that the finishing blow is
 * credited to the match rather than to the synchronization that found it.
 */
@ExtendWith(MockitoExtension.class)
class CampaignReplayInputAssemblerTest {

    /**
     * First operator of the frozen roster.
     */
    private static final Player ALPHA = CampaignFixtures.player(1, "Alpha");

    /**
     * Second operator of the frozen roster.
     */
    private static final Player BRAVO = CampaignFixtures.player(2, "Bravo");

    /**
     * Monday the campaign starts on.
     */
    private static final LocalDate MONDAY = CampaignFixtures.FIRST_WEEK_START;

    /**
     * Sunday the first week settles on.
     */
    private static final LocalDate SUNDAY = MONDAY.plusDays(6);

    @Mock
    private DailyOutputReader dailyOutputReader;

    @Mock
    private CampaignChallengeReader challengeReader;

    @Mock
    private CampaignPlayerRepository campaignPlayerRepository;

    private CampaignReplayInputAssembler assembler;

    private Campaign campaign;

    private List<CampaignWeek> weeks;

    @BeforeEach
    void setUp() {
        assembler = new CampaignReplayInputAssembler(
            dailyOutputReader,
            challengeReader,
            campaignPlayerRepository
        );
        campaign = CampaignFixtures.runningCampaign(1);
        weeks = List.of(CampaignFixtures.week(campaign, 1, 1_000, 50));

        when(campaignPlayerRepository.findAllByCampaignIdOrderByPlayerIdAsc(1L)).thenReturn(List.of(
            CampaignFixtures.member(campaign, ALPHA),
            CampaignFixtures.member(campaign, BRAVO)
        ));
        when(challengeReader.read(any(), anySet()))
            .thenReturn(Map.of(1, new WeekChallengeYield(12, Map.of(), Map.of())));
    }

    @Test
    @DisplayName("Credits the finishing blow to the match that landed it")
    void shouldCreditTheFinishingMatch() {
        stubOutput(List.of(
            valued(10L, 1L, MONDAY, 400),
            valued(20L, 2L, MONDAY.plusDays(1), 400),
            valued(30L, 1L, MONDAY.plusDays(2), 400)
        ));

        CampaignReplayInputs inputs = assembler.assemble(campaign, weeks, SUNDAY, SUNDAY);
        GuardianFight fight = inputs.fights().get(1);

        assertThat(fight.damageDealt()).isEqualTo(1_200);
        assertThat(fight.defeated()).isTrue();
        assertThat(fight.playerId()).isEqualTo(1L);
        assertThat(fight.playerMatchId()).isEqualTo(30L);
        assertThat(fight.defeatedAt()).isEqualTo(startOf(MONDAY.plusDays(2)));
    }

    @Test
    @DisplayName("Leaves a guardian standing while the week falls short")
    void shouldLeaveTheGuardianStanding() {
        stubOutput(List.of(valued(10L, 1L, MONDAY, 400)));

        GuardianFight fight = assembler.assemble(campaign, weeks, SUNDAY, SUNDAY).fights().get(1);

        assertThat(fight.damageDealt()).isEqualTo(400);
        assertThat(fight.defeated()).isFalse();
        assertThat(fight.playerMatchId()).isNull();
    }

    @Test
    @DisplayName("Ignores a player the campaign never froze into its roster")
    void shouldIgnorePlayersOutsideTheRoster() {
        stubOutput(List.of(
            valued(10L, 1L, MONDAY, 400),
            valued(99L, 9L, MONDAY, 5_000)
        ));

        CampaignReplayInputs inputs = assembler.assemble(campaign, weeks, SUNDAY, SUNDAY);

        assertThat(inputs.fights().get(1).damageDealt()).isEqualTo(400);
        assertThat(inputs.days().getFirst().damage()).isEqualTo(400);
        assertThat(inputs.days().getFirst().presenceCount()).isEqualTo(1);
        assertThat(inputs.playerDays()).hasSize(1);
    }

    @Test
    @DisplayName("Reports every day of the campaign, including the ones nobody played")
    void shouldReportEveryDay() {
        stubOutput(List.of(valued(10L, 1L, MONDAY, 400)));

        CampaignReplayInputs inputs = assembler.assemble(campaign, weeks, SUNDAY, SUNDAY);

        assertThat(inputs.days()).hasSize(7);
        assertThat(inputs.days())
            .extracting(CampaignDayInput::day)
            .containsExactlyElementsOf(daysOfWeek());
        assertThat(inputs.days().getLast().damage()).isZero();
        assertThat(inputs.days().getLast().presenceCount()).isZero();
    }

    @Test
    @DisplayName("Settles a week only once its Sunday has been reached")
    void shouldSettleOnlyReachedWeeks() {
        stubOutput(List.of(valued(10L, 1L, MONDAY, 400)));

        CampaignReplayInputs midWeek = assembler.assemble(campaign, weeks, MONDAY.plusDays(2), MONDAY.plusDays(1));

        assertThat(midWeek.weeks()).isEmpty();
        assertThat(midWeek.fights()).containsOnlyKeys(1);
    }

    @Test
    @DisplayName("Plays a Sunday's matches without settling it while it is still being played")
    void shouldNotSettleASundayStillBeingPlayed() {
        stubOutput(List.of(valued(10L, 1L, SUNDAY, 400)));

        CampaignReplayInputs sunday = assembler.assemble(campaign, weeks, SUNDAY, SUNDAY.minusDays(1));

        assertThat(sunday.weeks()).isEmpty();
        assertThat(sunday.fights().get(1).damageDealt()).isEqualTo(400);
        assertThat(sunday.days()).hasSize(7);
    }

    @Test
    @DisplayName("Hands the engine the wounded the week's challenges brought back")
    void shouldCarryTheChallengeYieldIntoTheSettlement() {
        stubOutput(List.of(valued(10L, 1L, MONDAY, 400)));

        assertThat(assembler.assemble(campaign, weeks, SUNDAY, SUNDAY).weeks().getFirst().challengeRescued())
            .isEqualTo(12);
    }

    /**
     * Stubs the priced reading of the range the assembler asks for.
     *
     * @param matches valued matches of the range
     */
    private void stubOutput(List<ValuedMatch> matches) {
        Map<LocalDate, Map<Long, PlayerDayOutput>> byDay = new HashMap<>();

        for (ValuedMatch match : matches) {
            byDay.computeIfAbsent(match.day(), ignored -> new HashMap<>())
                .merge(
                    match.playerId(),
                    PlayerDayOutput.NONE.plus(match),
                    (total, one) -> total.plus(match)
                );
        }

        when(dailyOutputReader.read(anySet(), any(), any()))
            .thenReturn(new DailyOutput(byDay, Map.of(), matches));
    }

    /**
     * Builds one valued match.
     *
     * @param playerMatchId match identifier
     * @param playerId      player who played it
     * @param day           calendar day
     * @param damage        damage it dealt
     * @return the valued match
     */
    private ValuedMatch valued(long playerMatchId, long playerId, LocalDate day, int damage) {
        return new ValuedMatch(
            playerMatchId,
            playerId,
            startOf(day),
            day,
            damage,
            100,
            1,
            0,
            damage,
            damage / 2,
            damage - damage / 2
        );
    }

    /**
     * Returns the seven days of the first week.
     *
     * @return the days, Monday first
     */
    private List<LocalDate> daysOfWeek() {
        List<LocalDate> days = new ArrayList<>(7);

        for (int index = 0; index < 7; index++) {
            days.add(MONDAY.plusDays(index));
        }

        return days;
    }

    /**
     * Returns the instant a day starts at.
     *
     * @param day calendar day
     * @return the instant
     */
    private Instant startOf(LocalDate day) {
        return day.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
    }
}
