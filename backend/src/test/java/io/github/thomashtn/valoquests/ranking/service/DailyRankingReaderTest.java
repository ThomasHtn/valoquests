package io.github.thomashtn.valoquests.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.ranking.RankingFixtures;
import io.github.thomashtn.valoquests.ranking.dto.DailyRankingResponse;
import io.github.thomashtn.valoquests.ranking.dto.DailyRankingResponse.DailyRankingEntryResponse;
import io.github.thomashtn.valoquests.scoring.service.DailyOutputReader;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Verifies one day's board: who brought what, against the night before.
 */
@ExtendWith(MockitoExtension.class)
class DailyRankingReaderTest {

    /**
     * Day on the board.
     */
    private static final LocalDate DAY = RankingFixtures.WEEK_START.plusDays(2);

    /**
     * Active player with the lowest identifier.
     */
    private static final Player ALPHA = RankingFixtures.player(1, "Alpha", PlayerStatus.ACTIVE);

    /**
     * Second active player.
     */
    private static final Player BRAVO = RankingFixtures.player(2, "Bravo", PlayerStatus.ACTIVE);

    /**
     * Deactivated player, listed without a slot.
     */
    private static final Player CHARLIE = RankingFixtures.player(3, "Charlie", PlayerStatus.INACTIVE);

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private DailyOutputReader dailyOutputReader;

    @InjectMocks
    private DailyRankingReader reader;

    @Test
    @DisplayName("Ranks the day and states each player's gap with the day before")
    void shouldRankTheDayAgainstTheDayBefore() {
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(ALPHA, BRAVO));
        when(dailyOutputReader.read(any(), any(), any())).thenReturn(RankingFixtures.output(Map.of(
            ALPHA.getId(), Map.of(
                DAY.minusDays(1), RankingFixtures.dayOutput(800, 2, 3),
                DAY, RankingFixtures.dayOutput(500, 1, 4)
            ),
            BRAVO.getId(), Map.of(DAY, RankingFixtures.dayOutput(900, 2, 1))
        )));

        DailyRankingResponse board = reader.read(DAY);

        assertThat(board.day()).isEqualTo(DAY);
        assertThat(board.previousDay()).isEqualTo(DAY.minusDays(1));
        assertThat(board.playedPlayerCount()).isEqualTo(2);
        assertThat(board.rosterPlayerCount()).isEqualTo(2);
        assertThat(board.ranking()).extracting(DailyRankingEntryResponse::playerId).containsExactly(2L, 1L);

        DailyRankingEntryResponse alpha = board.ranking().get(1);
        assertThat(alpha.position()).isEqualTo(2);
        assertThat(alpha.damage()).isEqualTo(500);
        assertThat(alpha.food()).isEqualTo(150);
        assertThat(alpha.components()).isEqualTo(350);
        assertThat(alpha.streakDays()).isEqualTo(4);
        assertThat(alpha.streakAtStake()).isEqualTo(3);
        assertThat(alpha.previousDamage()).isEqualTo(800);
        assertThat(alpha.damageVariation()).isEqualTo(-300);
    }

    @Test
    @DisplayName("Gives a player who did not play a line at zero, with the streak they are defending")
    void shouldListAPlayerWhoDidNotPlay() {
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(ALPHA, BRAVO));
        when(dailyOutputReader.read(any(), any(), any())).thenReturn(RankingFixtures.output(Map.of(
            ALPHA.getId(), Map.of(DAY.minusDays(1), RankingFixtures.dayOutput(300, 1, 5))
        )));

        DailyRankingResponse board = reader.read(DAY);

        assertThat(board.playedPlayerCount()).isZero();
        assertThat(board.ranking()).hasSize(2);

        DailyRankingEntryResponse alpha = board.ranking().getFirst();
        assertThat(alpha.playerId()).isEqualTo(ALPHA.getId());
        assertThat(alpha.damage()).isZero();
        assertThat(alpha.matchCount()).isZero();
        assertThat(alpha.streakDays()).isZero();
        assertThat(alpha.streakAtStake()).isEqualTo(5);
        assertThat(alpha.damageVariation()).isEqualTo(-300);
    }

    @Test
    @DisplayName("Never lets a deactivated player take a slot or count in the turnout")
    void shouldKeepADeactivatedPlayerOffTheSlots() {
        when(playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED))
            .thenReturn(List.of(ALPHA, CHARLIE));
        when(dailyOutputReader.read(any(), any(), any())).thenReturn(RankingFixtures.output(Map.of(
            CHARLIE.getId(), Map.of(DAY, RankingFixtures.dayOutput(1_000, 3, 1))
        )));

        DailyRankingResponse board = reader.read(DAY);

        // Charlie brought the most and is listed first, but the only slot goes to Alpha.
        assertThat(board.ranking()).extracting(DailyRankingEntryResponse::playerId).containsExactly(3L, 1L);
        assertThat(board.ranking().get(0).position()).isNull();
        assertThat(board.ranking().get(1).position()).isEqualTo(1);
        assertThat(board.playedPlayerCount()).isZero();
        assertThat(board.rosterPlayerCount()).isEqualTo(1);
    }
}
