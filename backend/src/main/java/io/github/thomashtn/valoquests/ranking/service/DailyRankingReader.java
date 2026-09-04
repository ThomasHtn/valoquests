package io.github.thomashtn.valoquests.ranking.service;

import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.ranking.dto.DailyRankingResponse;
import io.github.thomashtn.valoquests.scoring.model.DailyOutput;
import io.github.thomashtn.valoquests.scoring.model.PlayerDayOutput;
import io.github.thomashtn.valoquests.scoring.service.DailyOutputReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prices one day on demand and ranks the roster on it.
 *
 * <p>Nothing is persisted at this scale. The day is read back off the stored matches through the
 * same reader the weekly ranking and the campaign use, so one evening is worth the same wherever it
 * is shown. The day before is read in the same pass, because a day's total says nothing without
 * last night to hold it against.
 *
 * <p>Every player of the roster gets a line, archived ones aside, whether they played or not: a zero
 * on an evening the rest of the squad played is exactly what this board exists to show. Only the
 * competing squad takes a slot and counts towards the turnout.
 */
@Service
@Transactional(readOnly = true)
public class DailyRankingReader {

    /**
     * Repository listing the roster the board draws a line for.
     */
    private final PlayerRepository playerRepository;

    /**
     * Reader pricing a day's matches.
     */
    private final DailyOutputReader dailyOutputReader;

    /**
     * Creates the daily ranking reader.
     *
     * @param playerRepository  player repository
     * @param dailyOutputReader daily output reader
     */
    public DailyRankingReader(PlayerRepository playerRepository, DailyOutputReader dailyOutputReader) {
        this.playerRepository = playerRepository;
        this.dailyOutputReader = dailyOutputReader;
    }

    /**
     * Ranks one day.
     *
     * @param day day to rank
     * @return the day's board
     */
    public DailyRankingResponse read(LocalDate day) {
        LocalDate previous = day.minusDays(1);
        DailyOutput output = dailyOutputReader.read(
            EnumSet.complementOf(EnumSet.of(PlayerStatus.ARCHIVED)),
            previous,
            day
        );

        List<Player> ordered = new ArrayList<>(
            playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED)
        );
        ordered.sort(Comparator
            .comparingInt((Player player) -> output.of(player.getId(), day).damage()).reversed()
            .thenComparing(Player::getId));

        List<DailyRankingResponse.DailyRankingEntryResponse> ranking = new ArrayList<>(ordered.size());
        int position = 1;
        int played = 0;
        int competitors = 0;

        for (Player player : ordered) {
            PlayerDayOutput today = output.of(player.getId(), day);
            int previousDamage = output.of(player.getId(), previous).damage();

            if (player.isCompetitive()) {
                competitors++;
                played += today.matchCount() > 0 ? 1 : 0;
            }

            ranking.add(new DailyRankingResponse.DailyRankingEntryResponse(
                player.isCompetitive() ? position++ : null,
                player.getId(),
                player.getDisplayName(),
                player.getPortrait(),
                today.damage(),
                today.food(),
                today.components(),
                today.matchCount(),
                today.reducedMatchCount(),
                today.streakDays(),
                today.streakBonusPercent(),
                output.streakEndingOn(player.getId(), previous),
                previousDamage,
                today.damage() - previousDamage
            ));
        }

        return new DailyRankingResponse(day, previous, played, competitors, ranking);
    }
}
