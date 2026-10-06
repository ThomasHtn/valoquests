package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import io.github.thomashtn.valoquests.challenge.entity.ChallengeSelection;
import io.github.thomashtn.valoquests.challenge.exception.ChallengeDrawException;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeRepository;
import io.github.thomashtn.valoquests.challenge.repository.ChallengeSelectionRepository;
import io.github.thomashtn.valoquests.challenge.repository.PlayerChallengeProgressRepository;
import io.github.thomashtn.valoquests.scoring.model.ChallengeCadence;
import io.github.thomashtn.valoquests.scoring.model.ChallengeTier;
import io.github.thomashtn.valoquests.shared.exception.ConflictException;
import io.github.thomashtn.valoquests.shared.time.WeekCalendar;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Draws, reads and redraws deterministic weekly challenge packs.
 *
 * <p>One challenge per tier; existing selections are never replaced, {@link WeeklyPackDraw} picks the
 * missing ones.
 */
@Service
public class DefaultWeeklyChallengeDrawService implements WeeklyChallengeDrawService {

    /**
     * Number of challenges expected in one complete weekly pack.
     */
    private static final int WEEKLY_CHALLENGE_COUNT = ChallengeTier.values().length;

    /**
     * Application logger.
     */
    private static final Logger LOGGER =
        LoggerFactory.getLogger(DefaultWeeklyChallengeDrawService.class);

    /**
     * Challenge catalogue repository.
     */
    private final ChallengeRepository challengeRepository;

    /**
     * Repository of the challenge selections.
     */
    private final ChallengeSelectionRepository challengeSelectionRepository;

    /**
     * Progress repository, used by the redraw to clear the discarded pack's progress first.
     */
    private final PlayerChallengeProgressRepository progressRepository;

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
     * Creates the weekly challenge draw service.
     *
     * @param challengeRepository       challenge catalogue repository
     * @param challengeSelectionRepository challenge selection repository
     * @param progressRepository        player challenge progress repository
     * @param selectionFactory          factory resolving drawn challenges into selections
     * @param clock                     application clock
     * @param weekCalendar              calendar resolving the current week
     */
    public DefaultWeeklyChallengeDrawService(
        ChallengeRepository challengeRepository,
        ChallengeSelectionRepository challengeSelectionRepository,
        PlayerChallengeProgressRepository progressRepository,
        ChallengeSelectionFactory selectionFactory,
        Clock clock,
        WeekCalendar weekCalendar
    ) {
        this.challengeRepository = challengeRepository;
        this.challengeSelectionRepository = challengeSelectionRepository;
        this.progressRepository = progressRepository;
        this.selectionFactory = selectionFactory;
        this.clock = clock;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Retrieves or creates the challenge pack assigned to one week.
     *
     * @param weekStart Monday identifying the week
     * @return complete weekly challenge pack
     */
    @Override
    @Transactional
    public List<ChallengeSelection> selectWeekChallenges(LocalDate weekStart) {
        return selectWeekChallenges(weekStart, ChallengeDrawOrder.UNSALTED_DRAW);
    }

    /**
     * Retrieves or creates the challenge pack assigned to one week, under one draw salt.
     *
     * @param weekStart Monday identifying the week
     * @param drawSalt  salt mixed into the candidate order
     * @return complete weekly challenge pack
     */
    private List<ChallengeSelection> selectWeekChallenges(LocalDate weekStart, long drawSalt) {
        validateWeekStart(weekStart);

        List<ChallengeSelection> existingSelections = findWeeklyPack(weekStart);

        validateExistingSelections(weekStart, existingSelections);

        if (existingSelections.size() == WEEKLY_CHALLENGE_COUNT) {
            LOGGER.debug("Weekly challenge pack already exists for week {}.", weekStart);
            return sortSelections(existingSelections);
        }

        List<Challenge> missingChallenges =
            selectMissingChallenges(weekStart, existingSelections, drawSalt);
        List<ChallengeSelection> newSelections = createSelections(
            weekStart,
            missingChallenges,
            clock.instant()
        );

        challengeSelectionRepository.saveAll(newSelections);

        List<ChallengeSelection> completedPack = new ArrayList<>(WEEKLY_CHALLENGE_COUNT);
        completedPack.addAll(existingSelections);
        completedPack.addAll(newSelections);
        completedPack.sort(ChallengeSelection.EASIEST_FIRST);

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
    public List<ChallengeSelection> findExistingWeekChallenges(LocalDate weekStart) {
        validateWeekStart(weekStart);

        return challengeSelectionRepository.findAllByWeekStartOrderByIdAsc(weekStart);
    }

    /**
     * Retrieves the weekly pack of one week, without its daily draws.
     *
     * @param weekStart Monday identifying the week
     * @return weekly selections ordered by identifier
     */
    private List<ChallengeSelection> findWeeklyPack(LocalDate weekStart) {
        return challengeSelectionRepository.findAllByWeekStartAndCadenceOrderByIdAsc(
            weekStart,
            ChallengeCadence.WEEKLY
        );
    }

    /**
     * Discards the current week's pack, with its progress, and draws a new one.
     *
     * <p>Salted with the current instant so the new pack differs; the scheduled draw stays unsalted and
     * reproducible. The no-repeat cycle only replays earlier weeks, so it is unaffected.
     *
     * @return the newly drawn pack
     */
    @Override
    @Transactional
    public List<ChallengeSelection> redrawCurrentWeekChallenges() {
        LocalDate weekStart = weekCalendar.currentWeekStart();
        List<ChallengeSelection> discarded = findWeeklyPack(weekStart);

        validateRedrawable(weekStart, discarded);

        // Progress first, it references the pack; no derived deleteAllBy, which SpotBugs flags as mutable.
        progressRepository.deleteAll(
            progressRepository
                .findAllBySelectionWeekStartOrderByPlayerIdAscSelectionIdAsc(weekStart)
                .stream()
                .filter(progress ->
                    progress.getSelection().getCadence() == ChallengeCadence.WEEKLY)
                .toList()
        );
        challengeSelectionRepository.deleteAll(discarded);
        challengeSelectionRepository.flush();

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
     * <p>The rollover only freezes past weeks, so a frozen current pack means something already broke.
     *
     * @param weekStart Monday identifying the week being redrawn
     * @param selections the pack about to be discarded
     */
    private void validateRedrawable(LocalDate weekStart, List<ChallengeSelection> selections) {
        boolean finalized = selections.stream()
            .anyMatch(selection -> selection.getFinalizedAt() != null);

        if (finalized) {
            throw new ConflictException(
                "Week " + weekStart + " holds a finalized challenge pack and cannot be redrawn."
            );
        }
    }

    /**
     * Draws the challenges completing an existing weekly pack.
     *
     * @param weekStart          selected week
     * @param existingSelections already persisted selections
     * @param drawSalt           salt mixed into the candidate order
     * @return challenges required to complete the pack
     */
    private List<Challenge> selectMissingChallenges(
        LocalDate weekStart,
        List<ChallengeSelection> existingSelections,
        long drawSalt
    ) {
        return WeeklyPackDraw.missingChallenges(
            weekStart,
            drawSalt,
            challengeRepository.findAllByEnabledTrueAndCadenceOrderByIdAsc(ChallengeCadence.WEEKLY),
            existingSelections,
            challengeSelectionRepository
                .findAllByCadenceAndWeekStartLessThanOrderByWeekStartAsc(ChallengeCadence.WEEKLY, weekStart)
        )
            .orElseThrow(() -> createSelectionException(weekStart));
    }

    /**
     * Creates all weekly selections with one shared selection timestamp.
     *
     * @param weekStart     selected week
     * @param challenges    selected catalogue challenges
     * @param selectionTime selection timestamp
     * @return new weekly challenge entities
     */
    private List<ChallengeSelection> createSelections(
        LocalDate weekStart,
        List<Challenge> challenges,
        Instant selectionTime
    ) {
        return challenges.stream()
            .map(challenge -> selectionFactory.weekly(weekStart, challenge, selectionTime))
            .toList();
    }

    /**
     * Sorts a weekly pack by tier and returns an immutable copy.
     *
     * @param selections weekly challenge selections
     * @return sorted immutable selections
     */
    private List<ChallengeSelection> sortSelections(List<ChallengeSelection> selections) {
        return selections.stream()
            .sorted(ChallengeSelection.EASIEST_FIRST)
            .toList();
    }

    /**
     * Logs the completed weekly challenge pack.
     *
     * @param weekStart selected week
     * @param selections completed challenge pack
     */
    private void logSelection(LocalDate weekStart, List<ChallengeSelection> selections) {
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
                "Selected weekly challenge: tier={}, code={}, category={}.",
                challenge.getTier(),
                challenge.getCode(),
                challenge.getCategory()
            );
        });
    }

    /**
     * Ensures that an existing weekly pack contains no duplicate tier or excess entry.
     *
     * @param weekStart          selected week
     * @param existingSelections persisted selections
     */
    private void validateExistingSelections(
        LocalDate weekStart,
        List<ChallengeSelection> existingSelections
    ) {
        if (existingSelections.size() > WEEKLY_CHALLENGE_COUNT) {
            throw new ChallengeDrawException(
                "Week " + weekStart + " contains more challenges than the supported tier count."
            );
        }

        Set<ChallengeTier> tiers = EnumSet.noneOf(ChallengeTier.class);

        for (ChallengeSelection selection : existingSelections) {
            ChallengeTier tier = selection.getChallenge().getTier();

            if (!tiers.add(tier)) {
                throw new ChallengeDrawException(
                    "Week " + weekStart + " contains multiple challenges for tier " + tier + "."
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
    private ChallengeDrawException createSelectionException(LocalDate weekStart) {
        return new ChallengeDrawException(
            "A complete weekly challenge pack cannot be selected for week "
                + weekStart
                + ". Verify that every tier has at least one enabled challenge with a "
                + "compatible exclusion group."
        );
    }
}
