package io.github.thomashtn.valoquests.boss.service;

import io.github.thomashtn.valoquests.boss.entity.BossCatalogEntry;
import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.exception.WeeklyBossSelectionException;
import io.github.thomashtn.valoquests.boss.repository.BossCatalogEntryRepository;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import io.github.thomashtn.valoquests.scoring.ScoringRuleset;
import io.github.thomashtn.valoquests.scoring.ScoringRulesetRegistry;
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
 * <p>An existing selection is never replaced. The collective difficulty modifier and win streak used to
 * size the week's fight are derived from the most recently finalized encounter, so no separate mutable
 * state has to be kept in sync — the whole boss timeline can be rebuilt from persisted rows alone.
 */
@Service
public class DefaultWeeklyBossSelectionService implements WeeklyBossSelectionService {

    /**
     * Divisor turning a percentage into a ratio.
     */
    private static final double PERCENT_SCALE = 100.0;

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
     * Registry resolving the current damage ruleset.
     */
    private final ScoringRulesetRegistry rulesetRegistry;

    /**
     * Repository used to count the players the roster holds active.
     */
    private final PlayerRepository playerRepository;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the weekly boss selection service.
     *
     * @param catalogRepository    boss catalogue repository
     * @param encounterRepository  weekly boss encounter repository
     * @param rulesetRegistry      scoring ruleset registry
     * @param playerRepository     player repository
     * @param weekCalendar         calendar resolving the current week
     */
    public DefaultWeeklyBossSelectionService(
        BossCatalogEntryRepository catalogRepository,
        WeeklyBossEncounterRepository encounterRepository,
        ScoringRulesetRegistry rulesetRegistry,
        PlayerRepository playerRepository,
        WeekCalendar weekCalendar
    ) {
        this.catalogRepository = catalogRepository;
        this.encounterRepository = encounterRepository;
        this.rulesetRegistry = rulesetRegistry;
        this.playerRepository = playerRepository;
        this.weekCalendar = weekCalendar;
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

        BossCatalogEntry chosen = drawBoss(weekStart, catalog);
        ScoringRuleset ruleset = rulesetRegistry.current();
        Optional<WeeklyBossEncounter> previous = encounterRepository.findLatestFinalized();

        int activePlayerCount = (int) playerRepository.countByStatus(Player.COMPETITIVE_STATUS);
        int modifierPercent = resolveModifierPercent(previous, ruleset);
        int baseHp = ruleset.bossBaseHp(chosen.getCategory(), activePlayerCount);
        int carriedOverHp = resolveCarriedOverHp(previous, baseHp, ruleset);
        int effectiveHp = (int) Math.round(baseHp * modifierPercent / PERCENT_SCALE) + carriedOverHp;

        WeeklyBossEncounter encounter = new WeeklyBossEncounter();
        encounter.setWeekStart(weekStart);
        encounter.setBossCatalogEntry(chosen);
        encounter.setRulesetVersion(ruleset.version());
        encounter.setBaseHp(baseHp);
        encounter.setDifficultyModifierPercent(modifierPercent);
        encounter.setCarriedOverHp(carriedOverHp);
        encounter.setEffectiveHp(effectiveHp);

        WeeklyBossEncounter saved = encounterRepository.save(encounter);

        LOGGER.info(
            "Boss encounter prepared for week {}: boss={}, activePlayers={}, modifier={}%, "
                + "carriedOverHp={}, effectiveHp={}.",
            weekStart,
            chosen.getCode(),
            activePlayerCount,
            modifierPercent,
            carriedOverHp,
            effectiveHp
        );

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WeeklyBossEncounter> findExistingWeekBoss(LocalDate weekStart) {
        validateWeekStart(weekStart);

        return encounterRepository.findByWeekStart(weekStart);
    }

    /**
     * Draws the boss for a week, avoiding repetition until the whole catalogue has been cycled through.
     *
     * @param weekStart selected week
     * @param catalog   enabled catalogue entries
     * @return deterministically chosen boss
     */
    private BossCatalogEntry drawBoss(LocalDate weekStart, List<BossCatalogEntry> catalog) {
        Set<Long> usedInCurrentCycle = usedBossIdsInCurrentCycle(catalog.size());

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
     * @param catalogSize number of currently enabled catalogue entries
     * @return identifiers of bosses used since the last completed cycle
     */
    private Set<Long> usedBossIdsInCurrentCycle(int catalogSize) {
        Set<Long> usedIds = new HashSet<>();

        for (WeeklyBossEncounter encounter : encounterRepository.findAllByOrderByWeekStartAsc()) {
            usedIds.add(encounter.getBossCatalogEntry().getId());

            if (usedIds.size() >= catalogSize) {
                usedIds.clear();
            }
        }

        return usedIds;
    }

    /**
     * Resolves the difficulty modifier applied to the week being prepared, from the outcome of the most
     * recently finalized encounter.
     *
     * @param previous most recently finalized encounter, when the campaign has one
     * @param ruleset  ruleset the new week is being sized with
     * @return resolved modifier, in percent
     */
    private int resolveModifierPercent(Optional<WeeklyBossEncounter> previous, ScoringRuleset ruleset) {
        return previous
            .map(encounter -> ruleset.nextDifficultyModifierPercent(
                encounter.getDifficultyModifierPercent(),
                encounter.isDefeated()
            ))
            .orElseGet(ruleset::initialDifficultyModifierPercent);
    }

    /**
     * Resolves the hit points inherited from a predecessor that survived.
     *
     * <p>Only the immediately preceding encounter is looked at, and only when it survived: remainders
     * never stack across several bad weeks, which together with the ruleset's cap is what keeps a losing
     * streak from compounding into a boss nobody can reach.
     *
     * @param previous most recently finalized encounter, when the campaign has one
     * @param baseHp   base hit points of the boss being prepared
     * @param ruleset  ruleset the new week is being sized with
     * @return hit points to carry over, capped, or zero
     */
    private int resolveCarriedOverHp(
        Optional<WeeklyBossEncounter> previous,
        int baseHp,
        ScoringRuleset ruleset
    ) {
        int capPercent = ruleset.carriedOverHpCapPercent();

        if (capPercent <= 0) {
            return 0;
        }

        int cap = (int) Math.round(baseHp * capPercent / PERCENT_SCALE);

        return previous
            .filter(encounter -> !encounter.isDefeated())
            .map(encounter -> Math.min(encounter.remainingHp(), cap))
            .orElse(0);
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
