package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.dto.CurrentChallengesResponse;
import io.github.thomashtn.valoquests.challenge.entity.PlayerChallengeProgress;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDefinition;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.model.ProgressMode;
import io.github.thomashtn.valoquests.challenge.parser.ChallengeDefinitionParser;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides the collective progress exposed by the current challenges endpoint.
 */
@Service
@Transactional(readOnly = true)
public class DefaultChallengeQueryService implements ChallengeQueryService {

    /**
     * Repository used to retrieve the challenges selected for a week.
     */
    private final WeeklyChallengeRepository weeklyChallengeRepository;

    /**
     * Repository used to retrieve persisted player progress.
     */
    private final PlayerChallengeProgressRepository progressRepository;

    /**
     * Repository used to count players and resolve the latest synchronization.
     */
    private final PlayerRepository playerRepository;

    /**
     * Parser used to expose typed challenge-definition values.
     */
    private final ChallengeDefinitionParser definitionParser;

    /**
     * Barèmes saying what a challenge of each difficulty is worth.
     */
    private final ScoringRuleset ruleset;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the current-challenge query service.
     *
     * @param weeklyChallengeRepository weekly challenge repository
     * @param progressRepository        player progress repository
     * @param playerRepository          tracked-player repository
     * @param definitionParser          challenge-definition parser
     * @param ruleset                   scoring ruleset
     * @param weekCalendar       calendar resolving the current week
     */
    public DefaultChallengeQueryService(
        WeeklyChallengeRepository weeklyChallengeRepository,
        PlayerChallengeProgressRepository progressRepository,
        PlayerRepository playerRepository,
        ChallengeDefinitionParser definitionParser,
        ScoringRuleset ruleset,
        WeekCalendar weekCalendar
    ) {
        this.weeklyChallengeRepository = weeklyChallengeRepository;
        this.progressRepository = progressRepository;
        this.playerRepository = playerRepository;
        this.definitionParser = definitionParser;
        this.ruleset = ruleset;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Returns collective progress for every challenge of the current week.
     *
     * <p>Read-only, like the rest of this class. It used to need a writable transaction because
     * resolving the week's barème went through a resolver that lazily drew the week's boss encounter;
     * with a single unversioned ruleset there is nothing to resolve and nothing to insert.
     *
     * @return current-week challenge response
     */
    @Override
    public CurrentChallengesResponse findCurrent() {
        LocalDate weekStart = weekCalendar.currentWeekStart();
        List<WeeklyChallenge> weeklyChallenges = findWeeklyChallenges(weekStart);
        Map<Long, List<PlayerChallengeProgress>> progressByChallenge =
            groupProgressByChallenge(weekStart);
        int totalPlayers = Math.toIntExact(
            playerRepository.countByStatus(Player.COMPETITIVE_STATUS)
        );

        List<CurrentChallengesResponse.ChallengeProgressResponse> challenges =
            weeklyChallenges.stream()
                .map(weeklyChallenge -> toChallengeResponse(
                    weeklyChallenge,
                    progressByChallenge,
                    totalPlayers
                ))
                .toList();

        return new CurrentChallengesResponse(
            weekStart,
            weekStart.plusDays(6),
            findLastSuccessfulSynchronizationAt(),
            challenges
        );
    }

    /**
     * Retrieves active challenges for the requested week.
     *
     * @param weekStart Monday identifying the requested week
     * @return ordered weekly challenges
     */
    private List<WeeklyChallenge> findWeeklyChallenges(LocalDate weekStart) {
        return weeklyChallengeRepository
            .findAllByWeekStartAndFinalizedAtIsNullOrderByIdAsc(weekStart);
    }

    /**
     * Groups persisted progress rows by weekly challenge identifier.
     *
     * @param weekStart Monday identifying the requested week
     * @return progress rows indexed by weekly challenge identifier
     */
    private Map<Long, List<PlayerChallengeProgress>> groupProgressByChallenge(
        LocalDate weekStart
    ) {
        return progressRepository
            .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(
                weekStart
            )
            .stream()
            .collect(Collectors.groupingBy(
                progress -> progress.getWeeklyChallenge().getId()
            ));
    }

    /**
     * Converts a weekly challenge and its progress into an API response.
     *
     * <p>Damage is the challenge's base value for its difficulty, resolved through the ruleset rather
     * than stored per challenge. The squad bonus it can be multiplied by is not folded in: it depends on
     * how many players
     * have completed the challenge and would make the advertised figure move on its own during the
     * week. {@code completedPlayers} and {@code totalPlayers} are what the client renders it from.
     *
     * @param weeklyChallenge    weekly challenge to convert
     * @param progressByChallenge progress rows indexed by challenge identifier
     * @param totalPlayers       number of active players
     * @return challenge response
     */
    private CurrentChallengesResponse.ChallengeProgressResponse toChallengeResponse(
        WeeklyChallenge weeklyChallenge,
        Map<Long, List<PlayerChallengeProgress>> progressByChallenge,
        int totalPlayers
    ) {
        ChallengeDefinition definition = definitionParser.parse(
            weeklyChallenge.getChallenge()
        );
        List<PlayerChallengeProgress> progressRows = progressByChallenge
            .getOrDefault(weeklyChallenge.getId(), List.of());
        int completedPlayers = countCompletedPlayers(progressRows);

        ChallengeDifficulty difficulty = weeklyChallenge.getChallenge().getDifficulty();
        int baseDamage = ruleset.challengeDamage(difficulty);

        return new CurrentChallengesResponse.ChallengeProgressResponse(
            weeklyChallenge.getId(),
            weeklyChallenge.getChallenge().getName(),
            weeklyChallenge.getChallenge().getDescription(),
            difficulty,
            resolveMetricLabel(definition),
            resolveTargetValue(definition, progressRows),
            baseDamage,
            resolveTeamBonusPercent(difficulty, baseDamage, completedPlayers),
            completedPlayers,
            totalPlayers,
            calculateCompletionPercentage(completedPlayers, totalPlayers)
        );
    }

    /**
     * Expresses the squad bonus currently earned as a percentage of the challenge's base damage.
     *
     * <p>Derived from the ruleset rather than recomputed by the client: the client would otherwise carry
     * its own copy of the bonus ladder, which is precisely how the advertised damage and the awarded
     * damage came to disagree before.
     *
     * @param difficulty       challenge difficulty
     * @param baseDamage       challenge damage before the bonus
     * @param completedPlayers players who have completed it so far
     * @return bonus as a percentage of the base damage, zero when nothing is earned yet
     */
    private int resolveTeamBonusPercent(
        ChallengeDifficulty difficulty,
        int baseDamage,
        int completedPlayers
    ) {
        if (baseDamage <= 0) {
            return 0;
        }

        return Math.round(
            ruleset.challengeTeamBonus(difficulty, completedPlayers) * 100.0f / baseDamage
        );
    }

    /**
     * Counts completed progress rows belonging to a competitive (active) player.
     *
     * <p>An inactive player can still complete a challenge, but it must never inflate the
     * collective completion reported here: {@code totalPlayers} only counts active players, so
     * the numerator must stay consistent with it.
     *
     * @param progressRows progress rows to inspect
     * @return number of completed rows from active players
     */
    private int countCompletedPlayers(List<PlayerChallengeProgress> progressRows) {
        return Math.toIntExact(
            progressRows.stream()
                .filter(PlayerChallengeProgress::isCompleted)
                .filter(progress -> progress.getPlayer().isCompetitive())
                .count()
        );
    }

    /**
     * Builds the metric label exposed by the endpoint.
     *
     * @param definition parsed challenge definition
     * @return distinct metric names joined in definition order
     */
    private String resolveMetricLabel(ChallengeDefinition definition) {
        // A progression challenge and an absolute one can share a metric while asking opposite things:
        // "K/D of at least 1.20" against "K/D five percent above your own last month". The suffix is
        // what lets the client tell those two chips apart, and what stops the target being read as an
        // absolute value when it is a percentage gain.
        String suffix = definition.progressMode() == ProgressMode.BASELINE ? "_PROGRESS" : "";

        return definition.conditions().stream()
            .map(condition -> condition.metric().name() + suffix)
            .distinct()
            .collect(Collectors.joining(" + "));
    }

    /**
     * Resolves the target displayed for a simple or composite challenge.
     *
     * @param definition   parsed challenge definition
     * @param progressRows persisted progress rows
     * @return target value, or {@code null} when no composite target is stored
     */
    private BigDecimal resolveTargetValue(
        ChallengeDefinition definition,
        List<PlayerChallengeProgress> progressRows
    ) {
        if (definition.conditions().size() == 1) {
            return definition.singleCondition().target();
        }

        return progressRows.stream()
            .map(PlayerChallengeProgress::getTargetValue)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    /**
     * Calculates collective challenge completion as a percentage.
     *
     * @param completedPlayers number of players who completed the challenge
     * @param totalPlayers     number of active players
     * @return completion percentage rounded to two decimal places
     */
    private BigDecimal calculateCompletionPercentage(
        int completedPlayers,
        int totalPlayers
    ) {
        if (totalPlayers == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(completedPlayers)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(totalPlayers), 2, RoundingMode.HALF_UP);
    }

    /**
     * Resolves the most recent successful synchronization timestamp.
     *
     * @return latest timestamp, or {@code null} when no player was synchronized
     */
    private Instant findLastSuccessfulSynchronizationAt() {
        return playerRepository
            .findLatestSuccessfulSynchronizationAt()
            .orElse(null);
    }
}
