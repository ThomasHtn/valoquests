package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayerDay;
import io.github.thomashtn.valoquests.campaign.model.WeekChallengeYield;
import io.github.thomashtn.valoquests.campaign.model.WeeklyTitle;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerDayRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies the four weekly honours, and that a tie awards none of them.
 */
@ExtendWith(MockitoExtension.class)
class WeeklyTitleResolverTest {

    /**
     * First operator of the fixtures.
     */
    private static final Player ALPHA = CampaignFixtures.player(1, "Alpha");

    /**
     * Second operator of the fixtures.
     */
    private static final Player BRAVO = CampaignFixtures.player(2, "Bravo");

    @Mock
    private CampaignPlayerDayRepository playerDayRepository;

    @Mock
    private CampaignPlayerRepository campaignPlayerRepository;

    @Mock
    private CampaignChallengeReader challengeReader;

    @InjectMocks
    private WeeklyTitleResolver resolver;

    private Campaign campaign;

    @BeforeEach
    void setUp() {
        campaign = CampaignFixtures.runningCampaign(1);
        lenient().when(campaignPlayerRepository.findAllByCampaignIdOrderByPlayerIdAsc(1L)).thenReturn(List.of(
            CampaignFixtures.member(campaign, ALPHA),
            CampaignFixtures.member(campaign, BRAVO)
        ));
    }

    @Test
    @DisplayName("Awards each honour to the single operator who leads it")
    void shouldAwardEveryTitle() {
        stubDays(
            day(ALPHA, campaign.getFirstWeekStart(), 900, 100, 5),
            day(BRAVO, campaign.getFirstWeekStart(), 100, 900, 2)
        );
        stubChallenges(Map.of(1L, 3, 2L, 1));

        Map<WeeklyTitle, Long> titles = resolver.resolve(campaign, campaign.getFirstWeekStart());

        assertThat(titles)
            .containsEntry(WeeklyTitle.MECHANIC, 1L)
            .containsEntry(WeeklyTitle.QUARTERMASTER, 2L)
            .containsEntry(WeeklyTitle.REGULAR, 1L)
            .containsEntry(WeeklyTitle.SCOUT, 1L);
    }

    @Test
    @DisplayName("Awards nothing when two operators lead the same honour")
    void shouldAwardNothingOnATie() {
        stubDays(
            day(ALPHA, campaign.getFirstWeekStart(), 500, 500, 4),
            day(BRAVO, campaign.getFirstWeekStart(), 500, 500, 4)
        );
        stubChallenges(Map.of(1L, 2, 2L, 2));

        assertThat(resolver.resolve(campaign, campaign.getFirstWeekStart())).isEmpty();
    }

    @Test
    @DisplayName("Awards nothing for a week nobody played")
    void shouldAwardNothingOnAnEmptyWeek() {
        stubDays();
        stubChallenges(Map.of());

        assertThat(resolver.resolve(campaign, campaign.getFirstWeekStart())).isEmpty();
    }

    @Test
    @DisplayName("Keeps the longest streak reached during the week, not the last one")
    void shouldKeepTheLongestStreakOfTheWeek() {
        stubDays(
            day(ALPHA, campaign.getFirstWeekStart(), 100, 100, 6),
            day(ALPHA, campaign.getFirstWeekStart().plusDays(3), 100, 100, 1),
            day(BRAVO, campaign.getFirstWeekStart(), 100, 100, 4)
        );
        stubChallenges(Map.of());

        assertThat(resolver.resolve(campaign, campaign.getFirstWeekStart()))
            .containsEntry(WeeklyTitle.REGULAR, 1L);
    }

    /**
     * Stubs the operator days of the week.
     *
     * @param days days to return
     */
    private void stubDays(CampaignPlayerDay... days) {
        when(playerDayRepository.findAllByCampaignIdAndDayBetweenOrderByDayAsc(
            1L,
            campaign.getFirstWeekStart(),
            campaign.getFirstWeekStart().plusDays(6)
        )).thenReturn(List.of(days));
    }

    /**
     * Stubs how many challenges each operator validated that week.
     *
     * @param completions validations per operator
     */
    private void stubChallenges(Map<Long, Integer> completions) {
        when(challengeReader.read(any(), anySet()))
            .thenReturn(Map.of(1, new WeekChallengeYield(0, Map.of(), completions)));
    }

    /**
     * Builds one stored operator day.
     *
     * @param player     operator
     * @param day        calendar day
     * @param components components produced
     * @param food       food produced
     * @param streakDays streak reached that day
     * @return the row
     */
    private CampaignPlayerDay day(Player player, LocalDate day, int components, int food, int streakDays) {
        CampaignPlayerDay row = new CampaignPlayerDay();
        row.setCampaign(campaign);
        row.setPlayer(player);
        row.setDay(day);
        row.setComponents(components);
        row.setFood(food);
        row.setDamage(components + food);
        row.setStreakDays(streakDays);

        return row;
    }
}
