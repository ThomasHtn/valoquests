package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.exception.ChallengeDrawException;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeSelectionRepository;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.ToLongFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Draws one challenge per day from the daily pool, deterministically.
 *
 * <p>Each day gets a challenge not drawn in the twenty-seven days before it while the pool allows
 * it, the least recently drawn one otherwise.</p>
 */
@Service
public class DefaultDailyChallengeDrawService implements DailyChallengeDrawService {

    /**
     * Days before a draw during which a daily challenge is not drawn again.
     *
     * <p>Twenty-seven, so that a challenge comes back at the earliest twenty-eight days after its
     * last draw: exactly the size of the daily pool.
     */
    private static final int DAILY_NO_REPEAT_WINDOW_DAYS = 27;

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDailyChallengeDrawService.class);

    /**
     * Challenge catalogue repository.
     */
    private final ChallengeRepository challengeRepository;

    /**
     * Repository of the daily selections.
     */
    private final ChallengeSelectionRepository challengeSelectionRepository;

    /**
     * Factory resolving a drawn challenge's targets into a selection row.
     */
    private final ChallengeSelectionFactory selectionFactory;

    /**
     * Application clock used for selection timestamps.
     */
    private final Clock clock;

    /**
     * Calendar resolving the week a day belongs to.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the daily challenge draw service.
     *
     * @param challengeRepository          challenge catalogue repository
     * @param challengeSelectionRepository challenge selection repository
     * @param selectionFactory             factory resolving drawn challenges into selections
     * @param clock                        application clock
     * @param weekCalendar                 calendar resolving the week a day belongs to
     */
    public DefaultDailyChallengeDrawService(
        ChallengeRepository challengeRepository,
        ChallengeSelectionRepository challengeSelectionRepository,
        ChallengeSelectionFactory selectionFactory,
        Clock clock,
        WeekCalendar weekCalendar
    ) {
        this.challengeRepository = challengeRepository;
        this.challengeSelectionRepository = challengeSelectionRepository;
        this.selectionFactory = selectionFactory;
        this.clock = clock;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Returns the daily challenge of one day, drawing it when needed.
     *
     * <p>Deterministic like the weekly draw: the same day always orders the pool the same way, so a
     * restart between the draw and its commit cannot hand the day two different challenges.
     *
     * @param day day to draw for
     * @return the day's challenge
     */
    @Override
    @Transactional
    public ChallengeSelection selectDailyChallenge(LocalDate day) {
        Objects.requireNonNull(day, "Day must not be null.");

        Optional<ChallengeSelection> existing =
            challengeSelectionRepository.findByCadenceAndDay(ChallengeCadence.DAILY, day);

        if (existing.isPresent()) {
            return existing.get();
        }

        Challenge drawn = drawDailyChallenge(day);
        ChallengeSelection selection = challengeSelectionRepository.save(
            selectionFactory.daily(weekCalendar.weekStartOf(day), day, drawn, clock.instant())
        );

        LOGGER.info("Daily challenge {} drawn for {}.", drawn.getCode(), day);

        return selection;
    }

    /**
     * Retrieves the daily challenges of a range of days, drawing nothing.
     *
     * @param firstDay first day, inclusive
     * @param lastDay  last day, inclusive
     * @return drawn daily challenges, oldest first
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChallengeSelection> findDailyChallenges(LocalDate firstDay, LocalDate lastDay) {
        Objects.requireNonNull(firstDay, "First day must not be null.");
        Objects.requireNonNull(lastDay, "Last day must not be null.");

        return challengeSelectionRepository.findAllByCadenceAndDayBetweenOrderByDayAsc(
            ChallengeCadence.DAILY,
            firstDay,
            lastDay
        );
    }

    /**
     * Picks the challenge of one day from the daily pool.
     *
     * <p>Challenges drawn inside the no-repeat window are set aside first. When the whole pool sits
     * inside it, because the pool shrank below the window, the least recently drawn one comes back:
     * a day without a challenge would be a worse outcome than an early repeat.
     *
     * @param day day being drawn
     * @return drawn challenge
     */
    private Challenge drawDailyChallenge(LocalDate day) {
        List<Challenge> pool = challengeRepository
            .findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.DAILY);

        if (pool.isEmpty()) {
            throw new ChallengeDrawException(
                "No daily challenge can be drawn for " + day + ": the daily pool is empty."
            );
        }

        Map<Long, LocalDate> lastDrawnByChallengeId = new HashMap<>();

        for (ChallengeSelection recent : challengeSelectionRepository.findAllByCadenceAndDayBetweenOrderByDayAsc(
            ChallengeCadence.DAILY,
            day.minusDays(DAILY_NO_REPEAT_WINDOW_DAYS),
            day.minusDays(1)
        )) {
            lastDrawnByChallengeId.put(recent.getChallenge().getId(), recent.getDay());
        }

        ToLongFunction<Challenge> dayOrder =
            challenge -> ChallengeDrawOrder.of(day, challenge, ChallengeDrawOrder.UNSALTED_DRAW);

        return pool.stream()
            .filter(challenge -> !lastDrawnByChallengeId.containsKey(challenge.getId()))
            .min(Comparator.comparingLong(dayOrder))
            .orElseGet(() -> pool.stream()
                .min(Comparator
                    .comparing((Challenge challenge) -> lastDrawnByChallengeId.get(challenge.getId()))
                    .thenComparingLong(dayOrder))
                .orElseThrow());
    }
}
