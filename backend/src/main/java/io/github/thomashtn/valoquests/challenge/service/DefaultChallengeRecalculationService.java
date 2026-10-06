package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.model.CalculatedProgress;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.model.PlayerStatus;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recalculates challenge progress, weekly pack and daily draws alike, from persisted matches.
 *
 * <p>Every daily draw of the week is recalculated, since a late match can be stored after midnight.
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
     * Service used to calculate one player's progress on the week's selections.
     */
    private final ChallengeProgressCalculationService calculationService;

    /**
     * Writer storing calculated challenge progress.
     */
    private final PlayerChallengeProgressWriter progressWriter;

    /**
     * Listener told once the current week's progress is rebuilt, the ranking in practice.
     */
    private final CurrentWeekProgressListener progressListener;

    /**
     * Service used to prepare the active weekly challenge pack.
     */
    private final WeeklyChallengeDrawService weeklyDrawService;

    /**
     * Service used to draw today's challenge and read the week's daily draws.
     */
    private final DailyChallengeDrawService dailyDrawService;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the current-week challenge recalculation service.
     *
     * @param playerRepository            player repository
     * @param calculationService          challenge calculation service
     * @param progressWriter              progress writer
     * @param progressListener            listener told once the current week's progress is rebuilt
     * @param weeklyDrawService           weekly challenge draw service
     * @param dailyDrawService            daily challenge draw service
     * @param weekCalendar                calendar resolving the current week
     */
    public DefaultChallengeRecalculationService(
        PlayerRepository playerRepository,
        ChallengeProgressCalculationService calculationService,
        PlayerChallengeProgressWriter progressWriter,
        CurrentWeekProgressListener progressListener,
        WeeklyChallengeDrawService weeklyDrawService,
        DailyChallengeDrawService dailyDrawService,
        WeekCalendar weekCalendar
    ) {
        this.playerRepository = playerRepository;
        this.calculationService = calculationService;
        this.progressWriter = progressWriter;
        this.progressListener = progressListener;
        this.weeklyDrawService = weeklyDrawService;
        this.dailyDrawService = dailyDrawService;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Draws what the current week still lacks, then recalculates every tracked player's progress.
     *
     * <p>Uses stored matches only, never the Henrik API.
     */
    @Override
    @Transactional
    public void drawAndRecalculateCurrentWeek() {
        LocalDate weekStart = weekCalendar.currentWeekStart();
        LocalDate today = weekCalendar.today();

        // Selected so missing current draws get created; earlier days are only loaded, never drawn late.
        List<ChallengeSelection> selections = new ArrayList<>(
            weeklyDrawService.selectWeekChallenges(weekStart)
        );
        dailyDrawService.selectDailyChallenge(today);
        selections.addAll(dailyDrawService.findDailyChallenges(weekStart, today));

        recalculateWeek(weekStart, selections);

        progressListener.currentWeekProgressRecalculated();
    }

    /**
     * Recalculates one week's progress from the challenge pack it already owns.
     *
     * <p>Loaded rather than selected, so a week without a pack keeps none; the rollover rebuilds the ranking.
     */
    @Override
    @Transactional
    public void recalculateWeekProgress(LocalDate weekStart) {
        recalculateWeek(
            weekStart,
            weeklyDrawService.findExistingWeekChallenges(weekStart)
        );
    }

    /**
     * Rebuilds every tracked player's progress against one week's challenge pack.
     *
     * @param weekStart        Monday identifying the recalculated week
     * @param selections every selection of that week, weekly pack and daily draws
     */
    private void recalculateWeek(
        LocalDate weekStart,
        List<ChallengeSelection> selections
    ) {
        if (selections.isEmpty()) {
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
            selections.size()
        );

        int progressCount = 0;

        for (Player player : players) {
            progressCount += recalculatePlayerProgress(
                player,
                weekStart,
                selections
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
     * @param selections active weekly challenges
     * @return number of processed challenge progress records
     */
    private int recalculatePlayerProgress(
        Player player,
        LocalDate weekStart,
        List<ChallengeSelection> selections
    ) {
        List<CalculatedProgress> calculated =
            calculationService.calculateWeek(player, weekStart, selections);

        progressWriter.saveAll(player, calculated);

        return calculated.size();
    }
}
