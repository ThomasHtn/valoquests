package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.calculator.ChallengeProgressCalculatorRegistry;
import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.WeeklyChallenge;
import io.github.thomashtn.valoquests.challenge.exception.WeeklyChallengeSelectionException;
import io.github.thomashtn.valoquests.challenge.model.ChallengeCadence;
import io.github.thomashtn.valoquests.challenge.model.ChallengeDifficulty;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.challenge.repository.WeeklyChallengeRepository;
import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToLongFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates and retrieves deterministic weekly challenge packs.
 *
 * <p>A complete pack contains exactly one challenge for every supported difficulty. Existing
 * selections are never replaced during the week. Category diversity is preferred, while
 * exclusion groups are always enforced.</p>
 *
 * <p>A challenge does not come back until its difficulty has been cycled through: each tier keeps
 * its own no-repeat cycle, reset once every one of its enabled challenges has been drawn. It is a
 * preference, not a constraint — a week that can only be filled by reusing a challenge is filled
 * that way rather than left incomplete.</p>
 *
 * <p>Days are drawn the same way from their own pool: one challenge per day, never one drawn in the
 * twenty-seven days before while the pool allows it, the least recently drawn otherwise.</p>
 */
@Service
public class DefaultWeeklyChallengeSelectionService implements WeeklyChallengeSelectionService {

    /**
     * Number of challenges expected in one complete weekly pack.
     */
    private static final int WEEKLY_CHALLENGE_COUNT = ChallengeDifficulty.values().length;

    /**
     * Days before a draw during which a daily challenge is not drawn again.
     *
     * <p>Twenty-seven, so that a challenge comes back at the earliest twenty-eight days after its
     * last draw: exactly the size of the daily pool.
     */
    private static final int DAILY_NO_REPEAT_WINDOW_DAYS = 27;

    /**
     * Orders persisted selections from the easiest to the hardest challenge.
     */
    private static final Comparator<WeeklyChallenge> WEEKLY_CHALLENGE_COMPARATOR =
        Comparator.comparingInt(selection -> selection.getChallenge().getDifficulty().ordinal());

    /**
     * Application logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(DefaultWeeklyChallengeSelectionService.class);

    /**
     * Challenge catalogue repository.
     */
    private final ChallengeRepository challengeRepository;

    /**
     * Weekly challenge repository.
     */
    private final WeeklyChallengeRepository weeklyChallengeRepository;

    /**
     * Progress repository, used by the redraw to clear the discarded pack's progress first.
     */
    private final PlayerChallengeProgressRepository progressRepository;

    /**
     * Registry used to exclude challenges that cannot currently be calculated.
     */
    private final ChallengeProgressCalculatorRegistry calculatorRegistry;

    /**
     * Factory resolving a drawn challenge's targets into a selection row.
     */
    private final ChallengeSelectionFactory selectionFactory;

    /**
     * Application clock used for week resolution and selection timestamps.
     */
    private final Clock clock;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the weekly challenge selection service.
     *
     * @param challengeRepository       challenge catalogue repository
     * @param weeklyChallengeRepository weekly challenge repository
     * @param progressRepository        player challenge progress repository
     * @param calculatorRegistry        challenge calculator registry
     * @param selectionFactory          factory resolving drawn challenges into selections
     * @param clock                     application clock
     * @param weekCalendar              calendar resolving the current week
     */
    public DefaultWeeklyChallengeSelectionService(
        ChallengeRepository challengeRepository,
        WeeklyChallengeRepository weeklyChallengeRepository,
        PlayerChallengeProgressRepository progressRepository,
        ChallengeProgressCalculatorRegistry calculatorRegistry,
        ChallengeSelectionFactory selectionFactory,
        Clock clock,
        WeekCalendar weekCalendar
    ) {
        this.challengeRepository = challengeRepository;
        this.weeklyChallengeRepository = weeklyChallengeRepository;
        this.progressRepository = progressRepository;
        this.calculatorRegistry = calculatorRegistry;
        this.selectionFactory = selectionFactory;
        this.clock = clock;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Selects the challenge pack for the current UTC week.
     *
     * @return current weekly challenges
     */
    @Override
    @Transactional
    public List<WeeklyChallenge> selectCurrentWeekChallenges() {
        return selectWeekChallenges(weekCalendar.currentWeekStart());
    }

    /**
     * Retrieves or creates the challenge pack assigned to one week.
     *
     * @param weekStart Monday identifying the week
     * @return complete weekly challenge pack
     */
    @Override
    @Transactional
    public List<WeeklyChallenge> selectWeekChallenges(LocalDate weekStart) {
        return selectWeekChallenges(weekStart, ChallengeDrawOrder.UNSALTED_DRAW);
    }

    /**
     * Retrieves or creates the challenge pack assigned to one week, under one draw salt.
     *
     * @param weekStart Monday identifying the week
     * @param drawSalt  salt mixed into the candidate order
     * @return complete weekly challenge pack
     */
    private List<WeeklyChallenge> selectWeekChallenges(LocalDate weekStart, long drawSalt) {
        validateWeekStart(weekStart);

        List<WeeklyChallenge> existingSelections = findWeeklyPack(weekStart);

        validateExistingSelections(weekStart, existingSelections);

        if (existingSelections.size() == WEEKLY_CHALLENGE_COUNT) {
            LOGGER.debug("Weekly challenge pack already exists for week {}.", weekStart);
            return sortSelections(existingSelections);
        }

        List<Challenge> missingChallenges =
            selectMissingChallenges(weekStart, existingSelections, drawSalt);
        List<WeeklyChallenge> newSelections = createWeeklyChallenges(
            weekStart,
            missingChallenges,
            clock.instant()
        );

        weeklyChallengeRepository.saveAll(newSelections);

        List<WeeklyChallenge> completedPack = new ArrayList<>(WEEKLY_CHALLENGE_COUNT);
        completedPack.addAll(existingSelections);
        completedPack.addAll(newSelections);
        completedPack.sort(WEEKLY_CHALLENGE_COMPARATOR);

        logSelection(weekStart, completedPack);
        return List.copyOf(completedPack);
    }

    /**
     * Retrieves every selection a week already owns, creating nothing.
     *
     * @param weekStart Monday identifying the week
     * @return the week's selections, empty when it never had any
     */
    @Override
    @Transactional(readOnly = true)
    public List<WeeklyChallenge> findExistingWeekChallenges(LocalDate weekStart) {
        validateWeekStart(weekStart);

        return weeklyChallengeRepository.findAllByWeekStartOrderByIdAsc(weekStart);
    }

    /**
     * Retrieves the weekly pack of one week, without its daily draws.
     *
     * @param weekStart Monday identifying the week
     * @return weekly selections ordered by identifier
     */
    private List<WeeklyChallenge> findWeeklyPack(LocalDate weekStart) {
        return weeklyChallengeRepository.findAllByWeekStartAndCadenceOrderByIdAsc(
            weekStart,
            ChallengeCadence.WEEKLY
        );
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
    public WeeklyChallenge selectDailyChallenge(LocalDate day) {
        Objects.requireNonNull(day, "Day must not be null.");

        Optional<WeeklyChallenge> existing =
            weeklyChallengeRepository.findByCadenceAndDay(ChallengeCadence.DAILY, day);

        if (existing.isPresent()) {
            return existing.get();
        }

        Challenge drawn = drawDailyChallenge(day);
        WeeklyChallenge selection = weeklyChallengeRepository.save(
            selectionFactory.daily(weekCalendar.weekStartOf(day), day, drawn, clock.instant())
        );

        LOGGER.info("Daily challenge {} drawn for {}.", drawn.getCode(), day);

        return selection;
    }

    /**
     * Retrieves the daily challenge of one day, drawing nothing.
     *
     * @param day day to look up
     * @return the day's challenge when it was drawn
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<WeeklyChallenge> findDailyChallenge(LocalDate day) {
        Objects.requireNonNull(day, "Day must not be null.");

        return weeklyChallengeRepository.findByCadenceAndDay(ChallengeCadence.DAILY, day);
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
    public List<WeeklyChallenge> findDailyChallenges(LocalDate firstDay, LocalDate lastDay) {
        Objects.requireNonNull(firstDay, "First day must not be null.");
        Objects.requireNonNull(lastDay, "Last day must not be null.");

        return weeklyChallengeRepository.findAllByCadenceAndDayBetweenOrderByDayAsc(
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
            .findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.DAILY)
            .stream()
            .filter(challenge -> calculatorRegistry.supports(challenge.getProgressMode()))
            .toList();

        if (pool.isEmpty()) {
            throw new WeeklyChallengeSelectionException(
                "No daily challenge can be drawn for " + day + ": the daily pool is empty."
            );
        }

        Map<Long, LocalDate> lastDrawnByChallengeId = new HashMap<>();

        for (WeeklyChallenge recent : weeklyChallengeRepository.findAllByCadenceAndDayBetweenOrderByDayAsc(
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

    /**
     * Discards the current week's pack, with its progress, and draws a new one.
     *
     * <p>Salted with the current instant, so the new pack differs from the one being discarded.
     * The scheduled draw stays unsalted and reproducible: it is what makes a week survive a
     * restart, whereas a redraw is by definition an operator overriding what that draw produced,
     * and one that gave the same five challenges back would be no redraw at all.
     *
     * <p>The no-repeat cycle is unaffected: it replays weeks strictly before this one, so the
     * discarded pack never counted towards it and the new one is not penalized by it.
     *
     * @return the newly drawn pack
     */
    @Override
    @Transactional
    public List<WeeklyChallenge> redrawCurrentWeekChallenges() {
        LocalDate weekStart = weekCalendar.currentWeekStart();
        List<WeeklyChallenge> discarded = findWeeklyPack(weekStart);

        validateRedrawable(weekStart, discarded);

        // Progress first: it references the pack. Loaded then deleted rather than through a derived
        // `deleteAllBy…`, which would make SpotBugs read the repository as mutable state everywhere.
        // The week's daily draws and their progress are not part of the pack and stay.
        progressRepository.deleteAll(
            progressRepository
                .findAllByWeeklyChallengeWeekStartOrderByPlayerIdAscWeeklyChallengeIdAsc(weekStart)
                .stream()
                .filter(progress ->
                    progress.getWeeklyChallenge().getCadence() == ChallengeCadence.WEEKLY)
                .toList()
        );
        weeklyChallengeRepository.deleteAll(discarded);
        weeklyChallengeRepository.flush();

        LOGGER.info(
            "Discarded {} challenge(s) of week {} for a manual redraw.",
            discarded.size(),
            weekStart
        );

        return selectWeekChallenges(weekStart, clock.instant().toEpochMilli());
    }

    /**
     * Refuses to redraw a pack that is already frozen.
     *
     * <p>The current week's pack is never finalized in a healthy state — the rollover only freezes
     * weeks strictly before the one in progress. Reaching this means a rollover closed the running
     * week, and rewriting its pack on top of that would compound the damage rather than repair it.
     *
     * @param weekStart Monday identifying the week being redrawn
     * @param selections the pack about to be discarded
     */
    private void validateRedrawable(LocalDate weekStart, List<WeeklyChallenge> selections) {
        boolean finalized = selections.stream()
            .anyMatch(selection -> selection.getFinalizedAt() != null);

        if (finalized) {
            throw new ConflictException(
                "Week " + weekStart + " holds a finalized challenge pack and cannot be redrawn."
            );
        }
    }

    /**
     * Selects challenges for all difficulty tiers missing from an existing weekly pack.
     *
     * @param weekStart          selected week
     * @param existingSelections already persisted selections
     * @return challenges required to complete the pack
     */
    private List<Challenge> selectMissingChallenges(
        LocalDate weekStart,
        List<WeeklyChallenge> existingSelections,
        long drawSalt
    ) {
        WeeklyPackSelectionState initialState = WeeklyPackSelectionState.from(existingSelections);
        List<ChallengeDifficulty> missingDifficulties = WeeklyPackSolver.missingDifficulties(initialState);

        if (missingDifficulties.isEmpty()) {
            return List.of();
        }

        Map<ChallengeDifficulty, List<Challenge>> candidatesByDifficulty =
            loadCandidatesByDifficulty(weekStart, drawSalt);

        // Challenges left in the current cycle first, the whole tier only as a fallback. The second
        // attempt is exactly the selection this service used to make on its own, so a week that
        // could be filled before is still filled now: no-repeat is a preference, never a reason to
        // hand out an incomplete pack.
        List<WeeklyChallenge> pastSelections = weeklyChallengeRepository
            .findAllByCadenceAndWeekStartLessThanOrderByWeekStartAsc(ChallengeCadence.WEEKLY, weekStart);

        return WeeklyPackSolver.solve(
            WeeklyChallengeCycle.withoutCurrentCycle(candidatesByDifficulty, pastSelections),
            missingDifficulties,
            initialState
        )
            .or(() -> WeeklyPackSolver.solve(candidatesByDifficulty, missingDifficulties, initialState))
            .orElseThrow(() -> createSelectionException(weekStart));
    }

    /**
     * Loads supported catalogue challenges and groups them by difficulty.
     *
     * <p>Each group uses a deterministic week-dependent order. The same week therefore produces
     * the same selection candidate order across application restarts.</p>
     *
     * @param weekStart selected week
     * @param drawSalt  salt mixed into the candidate order
     * @return eligible candidates grouped by difficulty
     */
    private Map<ChallengeDifficulty, List<Challenge>> loadCandidatesByDifficulty(
        LocalDate weekStart,
        long drawSalt
    ) {
        Map<ChallengeDifficulty, List<Challenge>> candidatesByDifficulty =
            new EnumMap<>(ChallengeDifficulty.class);

        for (ChallengeDifficulty difficulty : ChallengeDifficulty.values()) {
            candidatesByDifficulty.put(difficulty, new ArrayList<>());
        }

        challengeRepository.findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.WEEKLY)
            .stream()
            .filter(challenge -> calculatorRegistry.supports(challenge.getProgressMode()))
            .sorted(Comparator.comparingLong(challenge -> ChallengeDrawOrder.of(weekStart, challenge, drawSalt)))
            .forEach(challenge -> candidatesByDifficulty.get(challenge.getDifficulty()).add(challenge));

        return candidatesByDifficulty;
    }

    /**
     * Creates all weekly challenge entities with one shared selection timestamp.
     *
     * @param weekStart     selected week
     * @param challenges    selected catalogue challenges
     * @param selectionTime selection timestamp
     * @return new weekly challenge entities
     */
    private List<WeeklyChallenge> createWeeklyChallenges(
        LocalDate weekStart,
        List<Challenge> challenges,
        Instant selectionTime
    ) {
        return challenges.stream()
            .map(challenge -> selectionFactory.weekly(weekStart, challenge, selectionTime))
            .toList();
    }

    /**
     * Sorts a weekly pack by difficulty and returns an immutable copy.
     *
     * @param selections weekly challenge selections
     * @return sorted immutable selections
     */
    private List<WeeklyChallenge> sortSelections(List<WeeklyChallenge> selections) {
        return selections.stream()
            .sorted(WEEKLY_CHALLENGE_COMPARATOR)
            .toList();
    }

    /**
     * Logs the completed weekly challenge pack.
     *
     * @param weekStart selected week
     * @param selections completed challenge pack
     */
    private void logSelection(LocalDate weekStart, List<WeeklyChallenge> selections) {
        LOGGER.info(
            "Weekly challenge pack prepared for week {} with {} challenge(s).",
            weekStart,
            selections.size()
        );

        if (!LOGGER.isDebugEnabled()) {
            return;
        }

        selections.forEach(selection -> {
            Challenge challenge = selection.getChallenge();
            LOGGER.debug(
                "Selected weekly challenge: difficulty={}, code={}, category={}.",
                challenge.getDifficulty(),
                challenge.getCode(),
                challenge.getCategory()
            );
        });
    }

    /**
     * Ensures that an existing weekly pack contains no duplicate difficulty or excess entry.
     *
     * @param weekStart          selected week
     * @param existingSelections persisted selections
     */
    private void validateExistingSelections(
        LocalDate weekStart,
        List<WeeklyChallenge> existingSelections
    ) {
        if (existingSelections.size() > WEEKLY_CHALLENGE_COUNT) {
            throw new WeeklyChallengeSelectionException(
                "Week " + weekStart + " contains more challenges than the supported difficulty count."
            );
        }

        Set<ChallengeDifficulty> difficulties = EnumSet.noneOf(ChallengeDifficulty.class);

        for (WeeklyChallenge selection : existingSelections) {
            ChallengeDifficulty difficulty = selection.getChallenge().getDifficulty();

            if (!difficulties.add(difficulty)) {
                throw new WeeklyChallengeSelectionException(
                    "Week " + weekStart + " contains multiple challenges for difficulty " + difficulty + "."
                );
            }
        }
    }

    /**
     * Validates the requested week identifier.
     *
     * @param weekStart requested week start
     */
    private void validateWeekStart(LocalDate weekStart) {
        Objects.requireNonNull(weekStart, "Week start must not be null.");

        if (!weekCalendar.isWeekStart(weekStart)) {
            throw new IllegalArgumentException(
                "Weekly challenge selection must use a Monday as week start."
            );
        }
    }

    /**
     * Creates the exception raised when no compatible complete pack can be built.
     *
     * @param weekStart selected week
     * @return descriptive selection exception
     */
    private WeeklyChallengeSelectionException createSelectionException(LocalDate weekStart) {
        return new WeeklyChallengeSelectionException(
            "A complete weekly challenge pack cannot be selected for week "
                + weekStart
                + ". Verify that every difficulty has at least one enabled challenge with an "
                + "implemented progress calculator and compatible exclusion group."
        );
    }
}
