package io.github.thomashtn.valoquests.ranking.service;

import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.ranking.dto.DailyRankingResponse;
import io.github.thomashtn.valoquests.scoring.model.DailyOutput;
import io.github.thomashtn.valoquests.scoring.model.PlayerDayOutput;
import io.github.thomashtn.valoquests.scoring.service.DailyOutputReader;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
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
 * <p>Nothing is persisted: the day is priced by the same reader as the week and the campaign. Every
 * non-archived player gets a line, even at zero, but only the competing squad takes a slot.
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
     * Calendar placing a day in its week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the daily ranking reader.
     *
     * @param playerRepository  player repository
     * @param dailyOutputReader daily output reader
     * @param weekCalendar      calendar placing a day in its week
     */
    public DailyRankingReader(
        PlayerRepository playerRepository,
        DailyOutputReader dailyOutputReader,
        WeekCalendar weekCalendar
    ) {
        this.playerRepository = playerRepository;
        this.dailyOutputReader = dailyOutputReader;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Ranks one day.
     *
     * @param day day to rank
     * @return the day's board
     */
    public DailyRankingResponse read(LocalDate day) {
        DailyOutput output = dailyOutputReader.read(
            EnumSet.complementOf(EnumSet.of(PlayerStatus.ARCHIVED)),
            day,
            day
        );

        List<Player> ordered = new ArrayList<>(
            playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED)
        );
        ordered.sort(Comparator
            .comparingInt((Player player) -> output.of(player.getId(), day).damage()).reversed()
            .thenComparing(Player::getId));

        // No damage, no position.
        List<Integer> positions = CompetitionRanking.positions(
            ordered,
            player -> player.isCompetitive() && output.of(player.getId(), day).damage() > 0,
            player -> output.of(player.getId(), day).damage()
        );
        int competitors = (int) ordered.stream().filter(Player::isCompetitive).count();
        List<DailyRankingResponse.DailyRankingEntryResponse> ranking = new ArrayList<>(ordered.size());

        for (int index = 0; index < ordered.size(); index++) {
            Player player = ordered.get(index);
            PlayerDayOutput today = output.of(player.getId(), day);

            ranking.add(new DailyRankingResponse.DailyRankingEntryResponse(
                positions.get(index),
                player.isCompetitive(),
                player.getId(),
                player.getDisplayName(),
                player.getPortrait(),
                today.damage(),
                today.food(),
                today.components(),
                today.matchCount(),
                today.reducedMatchCount(),
                today.playedDays(),
                today.streakBonusPercent(),
                weekPlayedDays(output, player.getId(), day)
            ));
        }

        return new DailyRankingResponse(day, competitors, ranking);
    }

    /**
     * Lists the days of the week a player played, from Monday up to a day.
     *
     * @param output   reading covering the week up to that day
     * @param playerId internal player identifier
     * @param day      last day to look at, included
     * @return played days in ascending order
     */
    private List<LocalDate> weekPlayedDays(DailyOutput output, long playerId, LocalDate day) {
        List<LocalDate> played = new ArrayList<>();
        for (LocalDate current = weekCalendar.weekStartOf(day); !current.isAfter(day); current = current.plusDays(1)) {
            if (output.playedDaysUpTo(playerId, current) > 0) {
                played.add(current);
            }
        }

        return played;
    }
}
