package io.github.thomashtn.valorant.tracker.challenge.service;

import io.github.thomashtn.valorant.tracker.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valorant.tracker.challenge.calculator.PlayerChallengeContext;
import io.github.thomashtn.valorant.tracker.challenge.calculator.PlayerChallengeContextFactory;
import io.github.thomashtn.valorant.tracker.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valorant.tracker.player.entity.Player;
import io.github.thomashtn.valorant.tracker.player.model.PlayerStatus;
import io.github.thomashtn.valorant.tracker.player.repository.PlayerRepository;
import io.github.thomashtn.valorant.tracker.ranking.service.RankingRecalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Recalculates the current weekly challenge progress from persisted matches.
 */
@Service
public class DefaultChallengeRecalculationService
    implements ChallengeRecalculationService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(
            DefaultChallengeRecalculationService.class
        );

    /**
     * Repository used to retrieve active tracked players.
     */
    private final PlayerRepository playerRepository;

    /**
     * Factory used to load each player's eligible weekly matches.
     */
    private final PlayerChallengeContextFactory contextFactory;

    /**
     * Service used to calculate one challenge for one player.
     */
    private final ChallengeProgressCalculationService calculationService;

    /**
     * Service used to persist calculated challenge progress.
     */
    private final PlayerChallengeProgressPersistenceService
        persistenceService;

    /**
     * Service used to rebuild the current weekly ranking.
     */
    private final RankingRecalculationService rankingRecalculationService;

    /**
     * Service used to prepare the active weekly challenge pack.
     */
    private final WeeklyChallengeSelectionService
        weeklyChallengeSelectionService;

    /**
     * Application clock used to resolve the current UTC week.
     */
    private final Clock clock;

    /**
     * Creates the current-week challenge recalculation service.
     *
     * @param playerRepository                player repository
     * @param contextFactory                  player challenge context factory
     * @param calculationService              challenge calculation service
     * @param persistenceService              progress persistence service
     * @param rankingRecalculationService     ranking recalculation service
     * @param weeklyChallengeSelectionService weekly selection service
     * @param clock                           application clock
     */
    public DefaultChallengeRecalculationService(
        PlayerRepository playerRepository,
        PlayerChallengeContextFactory contextFactory,
        ChallengeProgressCalculationService calculationService,
        PlayerChallengeProgressPersistenceService persistenceService,
        RankingRecalculationService rankingRecalculationService,
        WeeklyChallengeSelectionService
            weeklyChallengeSelectionService,
        Clock clock
    ) {
        this.playerRepository = playerRepository;
        this.contextFactory = contextFactory;
        this.calculationService = calculationService;
        this.persistenceService = persistenceService;
        this.rankingRecalculationService = rankingRecalculationService;
        this.weeklyChallengeSelectionService =
            weeklyChallengeSelectionService;
        this.clock = clock;
    }

    /**
     * Recalculates every active player's progress for the current UTC week.
     *
     * <p>Only matches already stored in PostgreSQL are used. The Henrik API is
     * never called by this operation.</p>
     */
    @Override
    @Transactional
    public void recalculateCurrentWeekProgress() {
        LocalDate weekStart = resolveCurrentWeekStart();

        List<WeeklyChallenge> weeklyChallenges =
            weeklyChallengeSelectionService.selectWeekChallenges(
                weekStart
            );

        if (weeklyChallenges.isEmpty()) {
            LOGGER.info(
                "No active weekly challenges found for week {}.",
                weekStart
            );

            return;
        }

        List<Player> activePlayers =
            playerRepository.findAllByStatusOrderByIdAsc(
                PlayerStatus.ACTIVE
            );

        LOGGER.info(
            "Starting challenge progress recalculation for week {}: "
                + "{} player(s), {} challenge(s).",
            weekStart,
            activePlayers.size(),
            weeklyChallenges.size()
        );

        int progressCount = 0;

        for (Player player : activePlayers) {
            progressCount += recalculatePlayerProgress(
                player,
                weekStart,
                weeklyChallenges
            );
        }

        LOGGER.info(
            "Challenge progress recalculation completed for week {}. "
                + "{} progress record(s) processed.",
            weekStart,
            progressCount
        );

        rankingRecalculationService.recalculateCurrentRanking();
    }

    /**
     * Recalculates every weekly challenge for one active player.
     *
     * @param player           player being recalculated
     * @param weekStart        current week start
     * @param weeklyChallenges active weekly challenges
     * @return number of processed challenge progress records
     */
    private int recalculatePlayerProgress(
        Player player,
        LocalDate weekStart,
        List<WeeklyChallenge> weeklyChallenges
    ) {
        PlayerChallengeContext context =
            contextFactory.create(
                player,
                weekStart
            );

        LOGGER.debug(
            "Calculating {} challenge(s) for player {} using {} match(es).",
            weeklyChallenges.size(),
            player.getDisplayName(),
            context.playerMatches().size()
        );

        List<ChallengeProgressResult> results = weeklyChallenges.stream()
            .map(
                weeklyChallenge -> calculateProgress(
                    player,
                    weeklyChallenge,
                    context
                )
            )
            .toList();

        persistenceService.saveAll(
            player,
            weeklyChallenges,
            results
        );

        return results.size();
    }

    /**
     * Calculates and logs one weekly challenge result for a player.
     *
     * @param player          player being recalculated
     * @param weeklyChallenge evaluated weekly challenge
     * @param context         weekly player context
     * @return calculated progress result
     */
    private ChallengeProgressResult calculateProgress(
        Player player,
        WeeklyChallenge weeklyChallenge,
        PlayerChallengeContext context
    ) {
        ChallengeProgressResult result = calculationService.calculate(
            weeklyChallenge.getChallenge(),
            context
        );

        LOGGER.debug(
            "Calculated challenge {} for player {}: current={}, "
                + "target={}, completed={}.",
            weeklyChallenge.getChallenge().getCode(),
            player.getDisplayName(),
            result.currentValue(),
            result.targetValue(),
            result.completed()
        );

        return result;
    }

    /**
     * Resolves the Monday beginning the current UTC calendar week.
     *
     * @return current week start
     */
    private LocalDate resolveCurrentWeekStart() {
        return LocalDate
            .now(clock.withZone(ZoneOffset.UTC))
            .with(
                TemporalAdjusters.previousOrSame(
                    DayOfWeek.MONDAY
                )
            );
    }

}
