package io.github.thomashtn.valoquests.player.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.campaign.model.CampaignContribution;
import io.github.thomashtn.valoquests.campaign.model.CampaignStatus;
import io.github.thomashtn.valoquests.campaign.service.CampaignContributionReader;
import io.github.thomashtn.valoquests.player.dto.PlayerContributionResponse;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.exception.PlayerNotFoundException;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.ranking.RankingFixtures;
import io.github.thomashtn.valoquests.ranking.entity.WeeklyPlayerScore;
import io.github.thomashtn.valoquests.ranking.model.WeeklyTitle;
import io.github.thomashtn.valoquests.ranking.repository.WeeklyPlayerScoreRepository;
import io.github.thomashtn.valoquests.ranking.service.WeeklyTitleResolver;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies the contribution block: the player's week from the ranking, their campaign when live.
 */
@ExtendWith(MockitoExtension.class)
class PlayerContributionQueryServiceTest {

    /**
     * Player whose contribution is read.
     */
    private static final Player ALPHA = RankingFixtures.player(1, "Alpha", PlayerStatus.ACTIVE);

    /**
     * Another ranked player.
     */
    private static final Player BRAVO = RankingFixtures.player(2, "Bravo", PlayerStatus.ACTIVE);

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private WeeklyPlayerScoreRepository scoreRepository;

    @Mock
    private WeeklyTitleResolver titleResolver;

    @Mock
    private CampaignContributionReader campaignContributionReader;

    private PlayerContributionQueryService service;

    @BeforeEach
    void setUp() {
        service = new PlayerContributionQueryService(
            playerRepository,
            scoreRepository,
            titleResolver,
            campaignContributionReader,
            new WeekCalendar(Clock.fixed(RankingFixtures.MIDWEEK, ZoneOffset.UTC), ZoneOffset.UTC)
        );
    }

    @Test
    @DisplayName("Reads the week off the ranking row and the campaign off the reader")
    void shouldReadBothScales() {
        WeeklyPlayerScore alpha = RankingFixtures.score(ALPHA, 2, 1_100, 88);
        alpha.setStreakDays(3);
        alpha.setCompletedChallenges(2);
        WeeklyPlayerScore bravo = RankingFixtures.score(BRAVO, 1, 2_000, 0);
        when(playerRepository.existsById(ALPHA.getId())).thenReturn(true);
        when(scoreRepository.findAllByWeekStartOrderByPositionAsc(RankingFixtures.WEEK_START))
            .thenReturn(List.of(bravo, alpha));
        when(titleResolver.resolve(anyList())).thenReturn(Map.of(
            WeeklyTitle.REGULAR, ALPHA.getId(),
            WeeklyTitle.MECHANIC, BRAVO.getId()
        ));
        when(campaignContributionReader.read(ALPHA.getId())).thenReturn(Optional.of(new CampaignContribution(
            7, 1, CampaignStatus.RUNNING, 9_000, 3_000, 6_000, 40, 12, 5, 9, 60, 2, Map.of(WeeklyTitle.SCOUT, 1)
        )));

        PlayerContributionResponse response = service.findByPlayerId(ALPHA.getId());

        assertThat(response.playerId()).isEqualTo(ALPHA.getId());
        assertThat(response.week().weekStart()).isEqualTo(RankingFixtures.WEEK_START);
        assertThat(response.week().position()).isEqualTo(2);
        assertThat(response.week().guardianDamage()).isEqualTo(1_100);
        assertThat(response.week().challengePoints()).isEqualTo(88);
        assertThat(response.week().totalPoints()).isEqualTo(1_188);
        assertThat(response.week().streakDays()).isEqualTo(3);
        assertThat(response.week().completedChallenges()).isEqualTo(2);
        assertThat(response.week().titles()).containsExactly(WeeklyTitle.REGULAR);
        assertThat(response.campaign().campaignId()).isEqualTo(7);
        assertThat(response.campaign().damage()).isEqualTo(9_000);
        assertThat(response.campaign().finishingBlows()).isEqualTo(2);
        assertThat(response.campaign().titles()).containsExactly(Map.entry(WeeklyTitle.SCOUT, 1));
    }

    @Test
    @DisplayName("Answers null blocks before the week is built and outside any campaign")
    void shouldAnswerNullBlocksWhenNothingIsStored() {
        when(playerRepository.existsById(ALPHA.getId())).thenReturn(true);
        when(scoreRepository.findAllByWeekStartOrderByPositionAsc(RankingFixtures.WEEK_START)).thenReturn(List.of());
        when(titleResolver.resolve(anyList())).thenReturn(Map.of());
        when(campaignContributionReader.read(ALPHA.getId())).thenReturn(Optional.empty());

        PlayerContributionResponse response = service.findByPlayerId(ALPHA.getId());

        assertThat(response.week()).isNull();
        assertThat(response.campaign()).isNull();
    }

    @Test
    @DisplayName("Refuses an unknown player before reading anything")
    void shouldRejectAnUnknownPlayer() {
        when(playerRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.findByPlayerId(99L)).isInstanceOf(PlayerNotFoundException.class);
        verifyNoInteractions(scoreRepository, campaignContributionReader);
    }
}
