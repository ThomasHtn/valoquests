package io.github.thomashtn.valoquests.campaign.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.entity.CampaignPlayerDay;
import io.github.thomashtn.valoquests.campaign.entity.CampaignWeek;
import io.github.thomashtn.valoquests.campaign.model.CampaignContribution;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.model.WeekChallengeYield;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerDayRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignPlayerRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.repository.CampaignWeekRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.ranking.service.WeeklyTitleResolver;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies what one operator's campaign adds up to, and who gets nothing.
 */
@ExtendWith(MockitoExtension.class)
class CampaignContributionReaderTest {

    /**
     * Operator on the roster.
     */
    private static final Player ALPHA = CampaignFixtures.player(1, "Alpha");

    /**
     * Operator absent from the roster.
     */
    private static final Player BRAVO = CampaignFixtures.player(2, "Bravo");

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignPlayerRepository campaignPlayerRepository;

    @Mock
    private CampaignPlayerDayRepository playerDayRepository;

    @Mock
    private CampaignWeekRepository weekRepository;

    @Mock
    private CampaignChallengeReader challengeReader;

    @Mock
    private WeeklyPlayerScoreRepository scoreRepository;

    @Mock
    private WeeklyTitleResolver titleResolver;

    @InjectMocks
    private CampaignContributionReader reader;

    private Campaign campaign;

    @BeforeEach
    void setUp() {
        campaign = CampaignFixtures.runningCampaign(1);
        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.of(campaign));
    }

    @Test
    @DisplayName("Sums the operator's days, rescues, finishing blows and honours")
    void shouldSumTheOperatorsCampaign() {
        LocalDate first = CampaignFixtures.FIRST_WEEK_START;
        when(campaignPlayerRepository.existsByCampaignIdAndPlayerId(campaign.getId(), ALPHA.getId())).thenReturn(true);
        when(playerDayRepository.findAllByCampaignIdAndPlayerIdOrderByDayAsc(campaign.getId(), ALPHA.getId()))
            .thenReturn(List.of(
                day(first, 500, 2, 1),
                day(first.plusDays(1), 700, 3, 2),
                day(first.plusDays(2), 0, 0, 0)
            ));

        CampaignWeek defeated = CampaignFixtures.week(campaign, 1, 5_000, 50);
        defeated.setDefeatedByPlayer(ALPHA);
        CampaignWeek survived = CampaignFixtures.week(campaign, 2, 6_000, 60);
        when(weekRepository.findAllByCampaignIdOrderByWeekIndexAsc(campaign.getId()))
            .thenReturn(List.of(defeated, survived));

        when(challengeReader.read(campaign, Set.of(ALPHA.getId()))).thenReturn(Map.of(
            1, new WeekChallengeYield(12, Map.of(ALPHA.getId(), 12), Map.of(ALPHA.getId(), 3)),
            2, new WeekChallengeYield(5, Map.of(ALPHA.getId(), 5), Map.of(ALPHA.getId(), 1))
        ));

        WeeklyPlayerScore weekOne = new WeeklyPlayerScore();
        weekOne.setWeekStart(first);
        WeeklyPlayerScore weekTwo = new WeeklyPlayerScore();
        weekTwo.setWeekStart(first.plusWeeks(1));
        when(scoreRepository.findAllByWeekStartInOrderByWeekStartDescPositionAsc(any()))
            .thenReturn(List.of(weekTwo, weekOne));
        when(titleResolver.resolve(List.of(weekOne)))
            .thenReturn(Map.of(WeeklyTitle.MECHANIC, ALPHA.getId(), WeeklyTitle.SCOUT, BRAVO.getId()));
        when(titleResolver.resolve(List.of(weekTwo))).thenReturn(Map.of(WeeklyTitle.MECHANIC, ALPHA.getId()));

        CampaignContribution contribution = reader.read(ALPHA.getId()).orElseThrow();

        assertThat(contribution.campaignId()).isEqualTo(campaign.getId());
        assertThat(contribution.status()).isEqualTo(CampaignStatus.RUNNING);
        assertThat(contribution.damage()).isEqualTo(1_200);
        assertThat(contribution.food()).isEqualTo(360);
        assertThat(contribution.components()).isEqualTo(840);
        assertThat(contribution.matchCount()).isEqualTo(5);
        assertThat(contribution.activeDays()).isEqualTo(2);
        assertThat(contribution.longestStreak()).isEqualTo(2);
        assertThat(contribution.completedChallenges()).isEqualTo(4);
        assertThat(contribution.survivorsRescued()).isEqualTo(17);
        assertThat(contribution.finishingBlows()).isEqualTo(1);
        assertThat(contribution.titles()).containsExactly(Map.entry(WeeklyTitle.MECHANIC, 2));
    }

    @Test
    @DisplayName("Gives nothing to an operator the campaign did not freeze")
    void shouldGiveNothingOutsideTheRoster() {
        when(campaignPlayerRepository.existsByCampaignIdAndPlayerId(campaign.getId(), BRAVO.getId())).thenReturn(false);

        assertThat(reader.read(BRAVO.getId())).isEmpty();
        verifyNoInteractions(playerDayRepository, weekRepository, challengeReader);
    }

    @Test
    @DisplayName("Gives nothing between two campaigns")
    void shouldGiveNothingWithoutALiveCampaign() {
        when(campaignRepository.findByStatusNot(CampaignStatus.CLOSED)).thenReturn(Optional.empty());

        assertThat(reader.read(ALPHA.getId())).isEmpty();
        verifyNoInteractions(campaignPlayerRepository);
        verifyNoInteractions(titleResolver);
    }

    private CampaignPlayerDay day(LocalDate date, int damage, int matches, int streak) {
        CampaignPlayerDay day = new CampaignPlayerDay();
        day.setCampaign(campaign);
        day.setPlayer(ALPHA);
        day.setDay(date);
        day.setDamage(damage);
        day.setFood(damage * 3 / 10);
        day.setComponents(damage - damage * 3 / 10);
        day.setMatchCount(matches);
        day.setStreakDays(streak);

        return day;
    }
}
