package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
import io.github.thomashtn.valoquests.campaign.dto.CampaignPlayerDayResponse;
import io.github.thomashtn.valoquests.campaign.dto.CampaignTodayResponse;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignDailySnapshot;
import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayerDay;
import io.github.thomashtn.valoquests.campaign.model.WeeklyTitle;
import io.github.thomashtn.valoquests.campaign.repository.CampaignDailySnapshotRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerDayRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies the day in progress: what came in, who brought it, and what the base will eat tonight.
 */
@ExtendWith(MockitoExtension.class)
class CampaignDayReaderTest {

    /**
     * Day being read.
     */
    private static final LocalDate TODAY = CampaignFixtures.FIRST_WEEK_START.plusDays(2);

    @Mock
    private CampaignPlayerDayRepository playerDayRepository;

    @Mock
    private CampaignDailySnapshotRepository snapshotRepository;

    @Mock
    private WeeklyTitleResolver titleResolver;

    @InjectMocks
    private CampaignDayReader reader;

    private Campaign campaign;

    @BeforeEach
    void setUp() {
        campaign = CampaignFixtures.runningCampaign(1);
        when(titleResolver.resolve(any(), any())).thenReturn(Map.of(WeeklyTitle.MECHANIC, 1L));
    }

    @Test
    @DisplayName("Sums the day and lists its operators, most productive first")
    void shouldSumTheDayAndRankItsOperators() {
        when(playerDayRepository.findAllByCampaignIdAndDayBetweenOrderByDayAsc(1L, TODAY, TODAY))
            .thenReturn(List.of(
                day(CampaignFixtures.player(1, "Alpha"), 400, 120, 280, 2, 1, 3, 4),
                day(CampaignFixtures.player(2, "Bravo"), 900, 630, 270, 6, 2, 1, 0)
            ));
        when(snapshotRepository.findByCampaignIdAndDay(1L, TODAY)).thenReturn(Optional.of(snapshot(1_000)));

        CampaignTodayResponse today = reader.read(campaign, TODAY, CampaignFixtures.FIRST_WEEK_START);

        assertThat(today.day()).isEqualTo(TODAY);
        assertThat(today.damage()).isEqualTo(1_300);
        assertThat(today.food()).isEqualTo(750);
        assertThat(today.components()).isEqualTo(550);
        assertThat(today.presenceCount()).isEqualTo(2);
        assertThat(today.rosterSize()).isEqualTo(7);
        assertThat(today.dailyUpkeep()).isEqualTo(8);
        assertThat(today.players())
            .extracting(CampaignPlayerDayResponse::gameName)
            .containsExactly("Bravo", "Alpha");
        assertThat(today.titles()).containsEntry(WeeklyTitle.MECHANIC, 1L);
    }

    @Test
    @DisplayName("Reports both multipliers so the rules can be read on screen")
    void shouldReportBothMultipliers() {
        when(playerDayRepository.findAllByCampaignIdAndDayBetweenOrderByDayAsc(1L, TODAY, TODAY))
            .thenReturn(List.of(day(CampaignFixtures.player(1, "Alpha"), 400, 120, 280, 7, 2, 5, 8)));
        when(snapshotRepository.findByCampaignIdAndDay(1L, TODAY)).thenReturn(Optional.empty());

        CampaignPlayerDayResponse row = reader.read(campaign, TODAY, CampaignFixtures.FIRST_WEEK_START)
            .players()
            .getFirst();

        assertThat(row.matchCount()).isEqualTo(7);
        assertThat(row.reducedMatchCount()).isEqualTo(2);
        assertThat(row.streakDays()).isEqualTo(5);
        assertThat(row.streakBonusPercent()).isEqualTo(8);
        assertThat(row.tagLine()).isEqualTo("EUW");
    }

    @Test
    @DisplayName("Answers an empty day rather than nothing")
    void shouldAnswerAnEmptyDay() {
        when(playerDayRepository.findAllByCampaignIdAndDayBetweenOrderByDayAsc(1L, TODAY, TODAY))
            .thenReturn(List.of());
        when(snapshotRepository.findByCampaignIdAndDay(1L, TODAY)).thenReturn(Optional.empty());

        CampaignTodayResponse today = reader.read(campaign, TODAY, CampaignFixtures.FIRST_WEEK_START);

        assertThat(today.damage()).isZero();
        assertThat(today.presenceCount()).isZero();
        assertThat(today.dailyUpkeep()).isZero();
        assertThat(today.players()).isEmpty();
    }

    /**
     * Builds one stored operator day.
     *
     * @param player             operator
     * @param damage             damage dealt
     * @param food               food produced
     * @param components         components produced
     * @param matchCount         valued matches played
     * @param reducedMatchCount  matches priced below full value
     * @param streakDays         streak reached
     * @param streakBonusPercent bonus the streak earned
     * @return the row
     */
    private CampaignPlayerDay day(
        Player player,
        int damage,
        int food,
        int components,
        int matchCount,
        int reducedMatchCount,
        int streakDays,
        int streakBonusPercent
    ) {
        CampaignPlayerDay row = new CampaignPlayerDay();
        row.setCampaign(campaign);
        row.setPlayer(player);
        row.setDay(TODAY);
        row.setDamage(damage);
        row.setFood(food);
        row.setComponents(components);
        row.setMatchCount(matchCount);
        row.setReducedMatchCount(reducedMatchCount);
        row.setStreakDays(streakDays);
        row.setStreakBonusPercent(streakBonusPercent);

        return row;
    }

    /**
     * Builds one stored day of the base.
     *
     * @param population inhabitants at the close of the day
     * @return the snapshot
     */
    private CampaignDailySnapshot snapshot(int population) {
        CampaignDailySnapshot snapshot = new CampaignDailySnapshot();
        snapshot.setCampaign(campaign);
        snapshot.setDay(TODAY);
        snapshot.setPopulation(BigDecimal.valueOf(population));

        return snapshot;
    }
}
