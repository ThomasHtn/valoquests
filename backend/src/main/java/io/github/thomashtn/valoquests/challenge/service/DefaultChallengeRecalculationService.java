package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressResult;
import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContext;
import io.github.thomashtn.valoquests.challenge.calculator.PlayerChallengeContextFactory;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.ranking.service.RankingRecalculationService;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PlayerChallengeProgressPersistenceService persistenceService;

    /**
     * Service used to rebuild the current weekly ranking.
     */
    private final RankingRecalculationService rankingRecalculationService;

    /**
     * Service used to prepare the active weekly challenge pack.
     */
    private final WeeklyChallengeSelectionService weeklyChallengeSelectionService;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the current-week challenge recalculation service.
     *
     * @param playerRepository                player repository
     * @param contextFactory                  player challenge context factory
     * @param calculationService              challenge calculation service
     * @param persistenceService              progress persistence service
     * @param rankingRecalculationService     ranking recalculation service
     * @param weeklyChallengeSelectionService weekly selection service
     * @param weekCalendar                    calendar resolving the current week
     */
    public DefaultChallengeRecalculationService(
        PlayerRepository playerRepository,
        PlayerChallengeContextFactory contextFactory,
        ChallengeProgressCalculationService calculationService,
        PlayerChallengeProgressPersistenceService persistenceService,
        RankingRecalculationService rankingRecalculationService,
        WeeklyChallengeSelectionService weeklyChallengeSelectionService,
        WeekCalendar weekCalendar
    ) {
        this.playerRepository = playerRepository;
        this.contextFactory = contextFactory;
        this.calculationService = calculationService;
        this.persistenceService = persistenceService;
        this.rankingRecalculationService = rankingRecalculationService;
        this.weeklyChallengeSelectionService = weeklyChallengeSelectionService;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Recalculates every tracked player's progress for the current UTC week.
     *
     * <p>Only matches already stored in PostgreSQL are used. The Henrik API is
     * never called by this operation.</p>
     */
    @Override
    @Transactional
    public void recalculateCurrentWeekProgress() {
        LocalDate weekStart = weekCalendar.currentWeekStart();

        // Selected rather than loaded: the current pack is created when it does not exist yet.
        List<WeeklyChallenge> weeklyChallenges =
            weeklyChallengeSelectionService.selectWeekChallenges(
                weekStart
            );

        recalculateWeek(weekStart, weeklyChallenges);

        rankingRecalculationService.recalculateCurrentRanking();
    }

    /**
     * Recalculates one week's progress from the challenge pack it already owns.
     *
     * <p>The pack is loaded rather than selected, so a week that never had one keeps none. The
     * ranking is deliberately left to the caller: the closing week's ranking is rebuilt by the
     * rollover, inside the transaction that also freezes it.
     */
    @Override
    @Transactional
    public void recalculateWeekProgress(LocalDate weekStart) {
        recalculateWeek(
            weekStart,
            weeklyChallengeSelectionService.findExistingWeekChallenges(weekStart)
        );
    }

    /**
     * Rebuilds every tracked player's progress against one week's challenge pack.
     *
     * @param weekStart        Monday identifying the recalculated week
     * @param weeklyChallenges challenge pack assigned to that week
     */
    private void recalculateWeek(
        LocalDate weekStart,
        List<WeeklyChallenge> weeklyChallenges
    ) {
        if (weeklyChallenges.isEmpty()) {
            LOGGER.info(
                "No active weekly challenges found for week {}.",
                weekStart
            );

            return;
        }

        List<Player> players =
            playerRepository.findAllByStatusNotOrderByIdAsc(PlayerStatus.ARCHIVED);

        LOGGER.info(
            "Starting challenge progress recalculation for week {}: "
                + "{} player(s), {} challenge(s).",
            weekStart,
            players.size(),
            weeklyChallenges.size()
        );

        int progressCount = 0;

        for (Player player : players) {
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
    }

    /**
     * Recalculates every weekly challenge for one tracked player.
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

}
