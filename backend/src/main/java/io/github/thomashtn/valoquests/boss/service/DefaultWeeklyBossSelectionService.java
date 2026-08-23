package io.github.thomashtn.valoquests.boss.service;

import io.github.thomashtn.valoquests.boss.entity.BossCatalogEntry;
import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.exception.WeeklyBossSelectionException;
import io.github.thomashtn.valoquests.boss.repository.BossCatalogEntryRepository;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
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
     * Neutral starting value of the collective difficulty modifier, in percent.
     */
    private static final int INITIAL_MODIFIER_PERCENT = 100;

    /**
     * Modifier increase applied after the boss is defeated.
     */
    private static final int MODIFIER_INCREASE_ON_VICTORY = 5;

    /**
     * Modifier decrease applied after the boss survives.
     */
    private static final int MODIFIER_DECREASE_ON_SURVIVAL = 10;

    /**
     * Lower bound of the collective difficulty modifier, in percent.
     */
    private static final int MINIMUM_MODIFIER_PERCENT = 70;

    /**
     * Upper bound of the collective difficulty modifier, in percent.
     */
    private static final int MAXIMUM_MODIFIER_PERCENT = 130;

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
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the weekly boss selection service.
     *
     * @param catalogRepository    boss catalogue repository
     * @param encounterRepository  weekly boss encounter repository
     * @param rulesetRegistry      scoring ruleset registry
     * @param weekCalendar         calendar resolving the current week
     */
    public DefaultWeeklyBossSelectionService(
        BossCatalogEntryRepository catalogRepository,
        WeeklyBossEncounterRepository encounterRepository,
        ScoringRulesetRegistry rulesetRegistry,
        WeekCalendar weekCalendar
    ) {
        this.catalogRepository = catalogRepository;
        this.encounterRepository = encounterRepository;
        this.rulesetRegistry = rulesetRegistry;
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
        ModifierState modifierState = resolveModifierState();
        ScoringRuleset ruleset = rulesetRegistry.current();
        int baseHp = ruleset.bossBaseHp(chosen.getCategory());
        int effectiveHp = (int) Math.round(baseHp * modifierState.modifierPercent() / 100.0);

        WeeklyBossEncounter encounter = new WeeklyBossEncounter();
        encounter.setWeekStart(weekStart);
        encounter.setBossCatalogEntry(chosen);
        encounter.setRulesetVersion(ruleset.version());
        encounter.setBaseHp(baseHp);
        encounter.setDifficultyModifierPercent(modifierState.modifierPercent());
        encounter.setEffectiveHp(effectiveHp);

        WeeklyBossEncounter saved = encounterRepository.save(encounter);

        LOGGER.info(
            "Boss encounter prepared for week {}: boss={}, modifier={}%, effectiveHp={}.",
            weekStart,
            chosen.getCode(),
            modifierState.modifierPercent(),
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
     * @return resolved modifier state
     */
    private ModifierState resolveModifierState() {
        return encounterRepository.findLatestFinalized()
            .map(previous -> previous.isDefeated()
                ? new ModifierState(
                    clampModifier(previous.getDifficultyModifierPercent() + MODIFIER_INCREASE_ON_VICTORY))
                : new ModifierState(
                    clampModifier(previous.getDifficultyModifierPercent() - MODIFIER_DECREASE_ON_SURVIVAL)))
            .orElseGet(() -> new ModifierState(INITIAL_MODIFIER_PERCENT));
    }

    /**
     * Bounds a difficulty modifier to the supported range.
     *
     * @param modifierPercent unbounded modifier value
     * @return modifier clamped to [{@value #MINIMUM_MODIFIER_PERCENT}, {@value #MAXIMUM_MODIFIER_PERCENT}]
     */
    private int clampModifier(int modifierPercent) {
        return Math.clamp(modifierPercent, MINIMUM_MODIFIER_PERCENT, MAXIMUM_MODIFIER_PERCENT);
    }

    /**
     * Produces a stable weekly order for one boss candidate.
     *
     * @param weekStart selected week
     * @param entry     boss candidate
     * @return deterministic ordering value
     */
    private long selectionOrder(LocalDate weekStart, BossCatalogEntry entry) {
        return Objects.hash(weekStart, entry.getId(), entry.getCode());
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

    /**
     * Resolved collective difficulty modifier for the week being prepared.
     *
     * @param modifierPercent modifier to apply, already clamped
     */
    private record ModifierState(int modifierPercent) {
    }
}
