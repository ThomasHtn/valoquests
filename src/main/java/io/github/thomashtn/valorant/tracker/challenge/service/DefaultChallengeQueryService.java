package io.github.thomashtn.valorant.tracker.challenge.service;

import io.github.thomashtn.valorant.tracker.challenge.dto.CurrentChallengesResponse;
import io.github.thomashtn.valorant.tracker.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valorant.tracker.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valorant.tracker.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valorant.tracker.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valorant.tracker.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.model.PlayerStatus;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Provides the collective progress exposed by the current challenges endpoint. */
@Service
@Transactional(readOnly = true)
public class DefaultChallengeQueryService implements ChallengeQueryService {

    private final WeeklyChallengeRepository weeklyChallengeRepository;
    private final PlayerChallengeProgressRepository progressRepository;
    private final PlayerRepository playerRepository;
    private final ChallengeDefinitionParser definitionParser;
    private final Clock clock;

    public DefaultChallengeQueryService(
        WeeklyChallengeRepository weeklyChallengeRepository,
        PlayerChallengeProgressRepository progressRepository,
        PlayerRepository playerRepository,
        ChallengeDefinitionParser definitionParser,
        Clock clock
    ) {
        this.weeklyChallengeRepository = weeklyChallengeRepository;
        this.progressRepository = progressRepository;
        this.playerRepository = playerRepository;
        this.definitionParser = definitionParser;
        this.clock = clock;
    }

    @Override
    public CurrentChallengesResponse findCurrent() {
        LocalDate weekStart = LocalDate.now(clock).with(
            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        List<WeeklyChallenge> weeklyChallenges = weeklyChallengeRepository
            .findAllByWeekStartAndFinalizedAtIsNullOrderByIdAsc(weekStart);
        List<PlayerChallengeProgress> progressRows = progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(weekStart);
        int totalPlayers = playerRepository.findAllByStatusOrderByIdAsc(PlayerStatus.ACTIVE).size();
        var progressByChallenge = progressRows.stream().collect(Collectors.groupingBy(
            row -> row.getWeeklyChallenge().getId()
        ));
        Instant lastSuccessfulAt = playerRepository.findAll().stream()
            .map(Player::getLastSuccessfulSynchronizationAt)
            .filter(Objects::nonNull)
            .max(Instant::compareTo)
            .orElse(null);

        List<CurrentChallengesResponse.ChallengeProgressResponse> challenges = weeklyChallenges.stream()
            .map(weekly -> {
                var definition = definitionParser.parse(weekly.getChallenge());
                List<PlayerChallengeProgress> rows = progressByChallenge.getOrDefault(weekly.getId(), List.of());
                int completedPlayers = (int) rows.stream().filter(PlayerChallengeProgress::isCompleted).count();
                BigDecimal percentage = totalPlayers == 0
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(completedPlayers)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalPlayers), 2, RoundingMode.HALF_UP);
                String metric = definition.conditions().stream()
                    .map(condition -> condition.metric().name())
                    .distinct()
                    .collect(Collectors.joining(" + "));
                BigDecimal target = definition.conditions().size() == 1
                    ? definition.singleCondition().target()
                    : rows.stream().map(PlayerChallengeProgress::getTargetValue).filter(Objects::nonNull).findFirst().orElse(null);
                return new CurrentChallengesResponse.ChallengeProgressResponse(
                    weekly.getId(),
                    weekly.getChallenge().getName(),
                    weekly.getChallenge().getDescription(),
                    weekly.getChallenge().getDifficulty(),
                    metric,
                    target,
                    weekly.getChallenge().getPoints(),
                    completedPlayers,
                    totalPlayers,
                    percentage
                );
            })
            .toList();

        return new CurrentChallengesResponse(
            weekStart,
            weekStart.plusDays(6),
            lastSuccessfulAt,
            challenges
        );
    }
}
