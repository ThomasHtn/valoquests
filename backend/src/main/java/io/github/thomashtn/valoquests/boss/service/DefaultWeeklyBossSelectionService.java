package io.github.thomashtn.valoquests.boss.service;

import io.github.thomashtn.valoquests.boss.entity.BossCatalogEntry;
import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.exception.WeeklyBossSelectionException;
import io.github.thomashtn.valoquests.boss.repository.BossCatalogEntryRepository;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.run.service.RunService;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Draws a deterministic, non-repeating boss for each week.
 *
 * <p>The boss a week fights is never replaced once drawn. Its hit points come from the roster's own
 * measured output rather than from any state carried between weeks, so the whole boss timeline can be
 * rebuilt from persisted rows alone.
 */
@Service
public class DefaultWeeklyBossSelectionService implements WeeklyBossSelectionService {

    /**
     * Odd 64-bit constant separating consecutive weeks before diffusion (golden-ratio derived).
     */
    private static final long WEEK_SEED_MULTIPLIER = 0x9E3779B97F4A7C15L;

    /**
     * First SplitMix64 finalizer multiplier.
     */
    private static final long AVALANCHE_FIRST_MULTIPLIER = 0xBF58476D1CE4E5B9L;

    /**
     * Second SplitMix64 finalizer multiplier.
     */
    private static final long AVALANCHE_SECOND_MULTIPLIER = 0x94D049BB133111EBL;

    /**
     * First SplitMix64 finalizer shift.
     */
    private static final int AVALANCHE_FIRST_SHIFT = 30;

    /**
     * Second SplitMix64 finalizer shift.
     */
    private static final int AVALANCHE_SECOND_SHIFT = 27;

    /**
     * Closing SplitMix64 finalizer shift.
     */
    private static final int AVALANCHE_FINAL_SHIFT = 31;

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWeeklyBossSelectionService.class);

    /**
     * Boss catalogue repository.
     */
    private final BossCatalogEntryRepository catalogRepository;

    /**
     * Weekly boss encounter repository.
     */
    private final WeeklyBossEncounterRepository encounterRepository;

    /**
     * Barèmes every encounter is sized with.
     */
    private final ScoringRuleset ruleset;

    /**
     * Repository used to count the players the roster holds active.
     */
    private final PlayerRepository playerRepository;

    /**
     * Measures the per-player output a new fight is sized against.
     */
    private final BossCalibrationService calibrationService;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Resolves the run the campaign currently runs in.
     */
    private final RunService runService;

    /**
     * Creates the weekly boss selection service.
     *
     * @param catalogRepository   boss catalogue repository
     * @param encounterRepository weekly boss encounter repository
     * @param ruleset             scoring ruleset
     * @param playerRepository    player repository
     * @param calibrationService  boss calibration service
     * @param weekCalendar        calendar resolving the current week
     * @param runService          service resolving the run the campaign runs in
     */
    public DefaultWeeklyBossSelectionService(
        BossCatalogEntryRepository catalogRepository,
        WeeklyBossEncounterRepository encounterRepository,
        ScoringRuleset ruleset,
        PlayerRepository playerRepository,
        BossCalibrationService calibrationService,
        WeekCalendar weekCalendar,
        RunService runService
    ) {
        this.catalogRepository = catalogRepository;
        this.encounterRepository = encounterRepository;
        this.ruleset = ruleset;
        this.playerRepository = playerRepository;
        this.calibrationService = calibrationService;
        this.weekCalendar = weekCalendar;
        this.runService = runService;
    }

    @Override
    @Transactional
    public WeeklyBossEncounter selectCurrentWeekBoss() {
        return selectWeekBoss(weekCalendar.currentWeekStart());
    }

    @Override
    @Transactional
    public WeeklyBossEncounter selectWeekBoss(LocalDate weekStart) {
        validateWeekStart(weekStart);

        Optional<WeeklyBossEncounter> existing = encounterRepository.findByWeekStart(weekStart);
        if (existing.isPresent()) {
            LOGGER.debug("Boss encounter already exists for week {}.", weekStart);
            return existing.orElseThrow();
        }

        List<BossCatalogEntry> catalog = catalogRepository.findAllByEnabledTrueOrderByIdAsc();
        if (catalog.isEmpty()) {
            throw new WeeklyBossSelectionException(
                "No enabled boss is available in the catalogue for week " + weekStart + "."
            );
        }

        Run run = runService.ensureRunFor(weekStart);

        WeeklyBossEncounter encounter = new WeeklyBossEncounter();
        encounter.setWeekStart(weekStart);
        encounter.setRun(run);
        encounter.setBossCatalogEntry(drawBoss(weekStart, catalog, run));

        applySizing(encounter);

        return encounterRepository.save(encounter);
    }

    @Override
    @Transactional
    public Optional<WeeklyBossEncounter> resizeWeekBoss(LocalDate weekStart) {
        validateWeekStart(weekStart);

        Optional<WeeklyBossEncounter> existing = encounterRepository.findByWeekStart(weekStart)
            .filter(encounter -> encounter.getFinalizedAt() == null);

        if (existing.isEmpty()) {
            return Optional.empty();
        }

        WeeklyBossEncounter encounter = existing.orElseThrow();
        applySizing(encounter);

        return Optional.of(encounterRepository.save(encounter));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WeeklyBossEncounter> findExistingWeekBoss(LocalDate weekStart) {
        validateWeekStart(weekStart);

        return encounterRepository.findByWeekStart(weekStart);
    }

    /**
     * Sizes one encounter from its immediate predecessor and the roster as it currently stands.
     *
     * <p>Idempotent, and deliberately derived entirely from persisted rows: the same encounter can be
     * sized again once its predecessor is finalized, which is what repairs a week drawn lazily during
     * the window between a Monday's first page view and that Monday's rollover.
     *
     * <p>The run is deliberately not re-attached here, unlike the act it replaces. An act had to be
     * repaired on every sizing because it was only knowable once a match of it had been imported; a run
     * is resolved from the week's own date, so the one stamped at creation is already the right one and
     * can never need moving.
     *
     * @param encounter encounter to size, carrying its week and its drawn boss
     */
    private void applySizing(WeeklyBossEncounter encounter) {
        int activePlayerCount = (int) playerRepository.countByStatus(Player.COMPETITIVE_STATUS);
        int referenceDamagePerPlayer = calibrationService.referenceDamagePerPlayer();
        int effectiveHp = ruleset.bossHitPoints(
            encounter.getBossCatalogEntry().getCategory(),
            activePlayerCount,
            referenceDamagePerPlayer
        );

        encounter.setActivePlayerCount(activePlayerCount);
        encounter.setEffectiveHp(effectiveHp);

        LOGGER.info(
            "Boss encounter sized for week {}: boss={}, activePlayers={}, reference={}, effectiveHp={}.",
            encounter.getWeekStart(),
            encounter.getBossCatalogEntry().getCode(),
            activePlayerCount,
            referenceDamagePerPlayer,
            effectiveHp
        );
    }

    /**
     * Draws the boss for a week, avoiding repetition until the whole catalogue has been cycled through.
     *
     * @param weekStart selected week
     * @param catalog   enabled catalogue entries
     * @param run       run the campaign currently runs in
     * @return deterministically chosen boss
     */
    private BossCatalogEntry drawBoss(
        LocalDate weekStart,
        List<BossCatalogEntry> catalog,
        Run run
    ) {
        Set<Long> usedInCurrentCycle = usedBossIdsInCurrentCycle(catalog.size(), run);

        List<BossCatalogEntry> candidates = catalog.stream()
            .filter(entry -> !usedInCurrentCycle.contains(entry.getId()))
            .toList();

        // Defensive fallback only: the cycle-tracking logic above always clears itself before it can
        // exhaust every candidate, so this never triggers in practice.
        if (candidates.isEmpty()) {
            candidates = catalog;
        }

        return candidates.stream()
            .min(Comparator.comparingLong(entry -> selectionOrder(weekStart, entry)))
            .orElseThrow();
    }

    /**
     * Replays every past selection to determine which bosses were already used in the cycle still in
     * progress, resetting whenever a cycle completes.
     *
     * <p>Replayed over the campaign in progress, not over the whole history: a run starts a fresh
     * campaign, and one opening on only the bosses its predecessor had not reached yet would face a
     * shrinking catalogue instead of a new run.
     *
     * @param catalogSize number of currently enabled catalogue entries
     * @param run         run the campaign currently runs in
     * @return identifiers of bosses used since the last completed cycle
     */
    private Set<Long> usedBossIdsInCurrentCycle(int catalogSize, Run run) {
        Set<Long> usedIds = new HashSet<>();

        List<WeeklyBossEncounter> campaignEncounters =
            encounterRepository.findAllByRunIdOrderByWeekStartAsc(run.getId());

        for (WeeklyBossEncounter encounter : campaignEncounters) {
            usedIds.add(encounter.getBossCatalogEntry().getId());

            if (usedIds.size() >= catalogSize) {
                usedIds.clear();
            }
        }

        return usedIds;
    }

    /**
     * Produces a stable weekly order for one boss candidate.
     *
     * <p>The week has to be mixed into every candidate's value non-additively, for the same reason
     * {@code DefaultWeeklyChallengeSelectionService} already does it: a shared week term added to each
     * candidate shifts them all equally and leaves the sorted order untouched, so every week would draw
     * from the same position in the catalogue.
     *
     * @param weekStart selected week
     * @param entry     boss candidate
     * @return deterministic ordering value
     */
    private long selectionOrder(LocalDate weekStart, BossCatalogEntry entry) {
        long bossSeed = Objects.hash(entry.getId(), entry.getCode());

        return avalanche(weekStart.toEpochDay() * WEEK_SEED_MULTIPLIER + bossSeed);
    }

    /**
     * Spreads a seed over the whole {@code long} range so neighbouring seeds order unrelatedly.
     *
     * <p>SplitMix64 finalizer: a bijection, so two distinct seeds keep distinct ordering values.
     *
     * @param seed ordering seed
     * @return diffused ordering value
     */
    private static long avalanche(long seed) {
        long mixed = seed;
        mixed = (mixed ^ (mixed >>> AVALANCHE_FIRST_SHIFT)) * AVALANCHE_FIRST_MULTIPLIER;
        mixed = (mixed ^ (mixed >>> AVALANCHE_SECOND_SHIFT)) * AVALANCHE_SECOND_MULTIPLIER;
        return mixed ^ (mixed >>> AVALANCHE_FINAL_SHIFT);
    }

    /**
     * Validates the requested week identifier.
     *
     * @param weekStart requested week start
     */
    private void validateWeekStart(LocalDate weekStart) {
        Objects.requireNonNull(weekStart, "Week start must not be null.");

        if (!weekCalendar.isWeekStart(weekStart)) {
            throw new IllegalArgumentException("Weekly boss selection must use a Monday as week start.");
        }
    }

}
