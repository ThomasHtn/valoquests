package io.github.thomashtn.valoquests.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.CampaignFixtures;
import io.github.thomashtn.valoquests.campaign.entity.Campaign;
import io.github.thomashtn.valoquests.campaign.repository.CampaignRepository;
import io.github.thomashtn.valoquests.campaign.service.CampaignWeekCoverageSource;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.ranking.RankingFixtures;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Verifies who reigns as champion: the lone first of the latest finalized campaign week.
 */
@ExtendWith(MockitoExtension.class)
class WeekChampionResolverTest {

    /**
     * Latest finalized week.
     */
    private static final LocalDate LAST_WEEK = RankingFixtures.WEEK_START.minusWeeks(1);

    /**
     * First ranked player.
     */
    private static final Player ALPHA = RankingFixtures.player(1, "Alpha", PlayerStatus.ACTIVE);

    /**
     * Second ranked player.
     */
    private static final Player BRAVO = RankingFixtures.player(2, "Bravo", PlayerStatus.ACTIVE);

    @Mock
    private WeeklyPlayerScoreRepository scoreRepository;

    @Mock
    private CampaignRepository campaignRepository;

    private WeekChampionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new WeekChampionResolver(scoreRepository, new CampaignWeekCoverageSource(campaignRepository));
    }

    @Test
    @DisplayName("Crowns the lone first of the latest finalized campaign week")
    void shouldCrownTheLatestWeekWinner() {
        when(scoreRepository.findFinalizedWeekStarts(PageRequest.of(0, 1)))
            .thenReturn(new PageImpl<>(List.of(LAST_WEEK), PageRequest.of(0, 1), 1));
        when(scoreRepository.findAllByWeekStartOrderByPositionAscPlayerIdAsc(LAST_WEEK))
            .thenReturn(List.of(row(BRAVO, 1), row(ALPHA, 2)));
        when(campaignRepository.findAll()).thenReturn(List.of(campaignCovering(LAST_WEEK)));

        assertThat(resolver.reigningChampion()).isEqualTo(BRAVO.getId());
    }

    @Test
    @DisplayName("Crowns nobody before any week is finalized")
    void shouldCrownNobodyBeforeTheFirstFinalizedWeek() {
        when(scoreRepository.findFinalizedWeekStarts(PageRequest.of(0, 1))).thenReturn(Page.empty());

        assertThat(resolver.reigningChampion()).isNull();
    }

    @Test
    @DisplayName("Names the champion reigning over a week as the one crowned the week before")
    void shouldNameTheChampionOfThePreviousWeek() {
        when(scoreRepository.findAllByWeekStartOrderByPositionAscPlayerIdAsc(LAST_WEEK))
            .thenReturn(List.of(row(ALPHA, 1), row(BRAVO, 2)));
        when(campaignRepository.findAll()).thenReturn(List.of(campaignCovering(LAST_WEEK)));

        assertThat(resolver.championBefore(LAST_WEEK.plusWeeks(1))).isEqualTo(ALPHA.getId());
    }

    private static WeeklyPlayerScore row(Player player, int position) {
        WeeklyPlayerScore score = RankingFixtures.score(player, position, 1_000, 0);
        score.setWeekStart(LAST_WEEK);

        return score;
    }

    private static Campaign campaignCovering(LocalDate firstWeekStart) {
        Campaign campaign = CampaignFixtures.runningCampaign(1);
        campaign.setFirstWeekStart(firstWeekStart);
        campaign.setLastWeekStart(firstWeekStart.plusWeeks(9));

        return campaign;
    }
}
