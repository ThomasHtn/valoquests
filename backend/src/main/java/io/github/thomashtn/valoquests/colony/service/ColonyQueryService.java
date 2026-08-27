package io.github.thomashtn.valoquests.colony.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.boss.entity.WeeklyBossEncounter;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import io.github.thomashtn.valoquests.colony.dto.ColonyFoodDayResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyMilestoneResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyMoraleResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyRunHistoryResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyTierResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyTrajectoryPointResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyTrajectoryResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyWeekResponse;
import io.github.thomashtn.valoquests.colony.entity.ColonyDailySnapshot;
import io.github.thomashtn.valoquests.colony.model.ColonyTier;
import io.github.thomashtn.valoquests.colony.model.ColonyTierState;
import io.github.thomashtn.valoquests.colony.model.ColonyWeekOutcomeState;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.scoring.model.BossCategory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the colony off its snapshots.
 *
 * <p>Computes nothing the engine has not already decided: the two ceilings, what the town eats and the
 * speed morale buys all come back through {@link ColonyReplayEngine}, so the page and the replay can
 * never disagree on a formula.
 */
@Service
public class ColonyQueryService {

    /**
     * Divisor turning a ratio into a percentage.
     */
    private static final double PERCENT_SCALE = 100.0;

    /**
     * Days in a week, the unit a run is counted in.
     */
    private static final int DAYS_IN_WEEK = 7;

    /**
     * Steps of the ladder shown behind the town's own.
     *
     * <p>One. The panel's subject is what comes next; a long tail of names already crossed would push
     * the step being paid for off the bottom of a column this narrow.
     */
    private static final int LADDER_STEPS_BEHIND = 1;

    /**
     * Steps of the ladder shown ahead of the town's own.
     */
    private static final int LADDER_STEPS_AHEAD = 4;

    /**
     * Reader handing out the run in progress and the days it has lived.
     */
    private final ColonyRunReader runReader;

    /**
     * Reader naming the day's turnout.
     */
    private final ColonyPresenceReader presenceReader;

    /**
     * Reader pricing what a week hands over, used here for the week still open.
     */
    private final ColonyMaterialsReader materialsReader;

    /**
     * Repository holding the run's fights.
     */
    private final WeeklyBossEncounterRepository encounterRepository;

    /**
     * Calibration the thresholds and bounds are read from.
     */
    private final ColonyRuleset ruleset;

    /**
     * Engine supplying the formulas the page displays.
     */
    private final ColonyReplayEngine engine;

    /**
     * Creates the colony query service.
     *
     * @param runReader           colony run reader
     * @param presenceReader      colony presence reader
     * @param materialsReader     colony materials reader
     * @param encounterRepository weekly boss encounter repository
     * @param ruleset             colony ruleset
     * @param engine              colony replay engine
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public ColonyQueryService(
        ColonyRunReader runReader,
        ColonyPresenceReader presenceReader,
        ColonyMaterialsReader materialsReader,
        WeeklyBossEncounterRepository encounterRepository,
        ColonyRuleset ruleset,
        ColonyReplayEngine engine
    ) {
        this.runReader = runReader;
        this.presenceReader = presenceReader;
        this.materialsReader = materialsReader;
        this.encounterRepository = encounterRepository;
        this.ruleset = ruleset;
        this.engine = engine;
    }

    /**
     * Returns the colony as it stands today.
     *
     * @return today's colony
     */
    @Transactional
    public ColonyResponse findCurrent() {
        Run run = runReader.currentRun();
        List<ColonyDailySnapshot> snapshots = runReader.snapshotsOf(run);
        ColonyDailySnapshot today = snapshots.getLast();

        double foodStock = today.getFoodStock().doubleValue();
        double population = today.getPopulation().doubleValue();
        double efficiency = today.getEfficiency().doubleValue();
        double consumption = engine.weeklyConsumption(population, efficiency);

        int rosterSize = run.getRosterSize();
        LocalDate weekStart = runReader.weekStartOf(today.getDay());

        ColonyTier tier = ruleset.tierFor(efficiency);
        ColonyTier nextTier = ruleset.nextTierFor(efficiency);

        return new ColonyResponse(
            run.getNumber(),
            runReader.runDayOf(run, today.getDay()),
            runDayCount(),
            runReader.runWeekOf(run, today.getDay()),
            ruleset.runLengthWeeks(),
            today.getDay(),
            rounded(population),
            rounded(today.getPopulationChange().doubleValue()),
            efficiency,
            today.getMaterials(),
            pendingMaterials(weekStart, rosterSize),
            foodStock,
            foodWindow(snapshots),
            ruleset.foodWindowDays(),
            rounded(engine.feedablePopulation(foodStock, efficiency)),
            consumption,
            Math.max(0.0, foodStock - consumption),
            presenceReader.read(today.getDay(), today.getPresenceCount(), rosterSize),
            morale(today.getMorale().doubleValue()),
            toTier(tier, ColonyTierState.CURRENT, rosterSize),
            toTier(nextTier, ColonyTierState.LOCKED, rosterSize),
            (efficiency - tier.threshold()) * PERCENT_SCALE / ruleset.efficiencyTierStep(),
            ladder(tier, rosterSize),
            weeks(run, weekStart),
            defeatedBosses(run),
            ruleset.runLengthWeeks()
        );
    }

    /**
     * Returns what the week still open has already secured, and which Monday will credit.
     *
     * <p>Read through the same reader the rollover itself uses, which filters an encounter on its
     * finalization: an open week therefore contributes its validated challenges and nothing else, since
     * its fight has not been settled and pays nothing yet. What that fight <b>would</b> pay is on its
     * own tile, quoted as what is on the table.
     *
     * @param weekStart  Monday of the week today falls in
     * @param rosterSize roster size frozen on the run
     * @return materials the next rollover will credit
     */
    private int pendingMaterials(LocalDate weekStart, int rosterSize) {
        return materialsReader.outcomeOf(weekStart, rosterSize).materials();
    }

    /**
     * Returns the seven daily harvests the food stock is made of, oldest first.
     *
     * <p>A slice of the snapshots already loaded, so it costs nothing. Shorter than seven days at the
     * very start of a run, which is correct: the window fills as the run does.
     *
     * @param snapshots the run's snapshots, oldest day first
     * @return the window's days
     */
    private List<ColonyFoodDayResponse> foodWindow(List<ColonyDailySnapshot> snapshots) {
        return snapshots.stream()
            .skip(Math.max(0, snapshots.size() - ruleset.foodWindowDays()))
            .map(snapshot -> new ColonyFoodDayResponse(
                snapshot.getDay(),
                snapshot.getFoodHarvest().doubleValue()
            ))
            .toList();
    }

    /**
     * Returns the population curve of the run in progress.
     *
     * @return the run's curve, with its peak, its average and the days it changed name
     */
    @Transactional
    public ColonyTrajectoryResponse findTrajectory() {
        Run run = runReader.currentRun();
        List<ColonyDailySnapshot> snapshots = runReader.snapshotsOf(run);

        ColonyDailySnapshot peak = snapshots.stream()
            .max(Comparator.comparing(ColonyDailySnapshot::getPopulation))
            .orElseThrow();

        int average = rounded(snapshots.stream()
            .mapToDouble(snapshot -> snapshot.getPopulation().doubleValue())
            .average()
            .orElse(0.0));

        return new ColonyTrajectoryResponse(
            run.getNumber(),
            runDayCount(),
            rounded(peak.getPopulation().doubleValue()),
            peak.getDay(),
            average,
            snapshots.stream().map(snapshot -> toPoint(run, snapshot)).toList(),
            milestones(snapshots, run)
        );
    }

    /**
     * Returns every closed run, most recent first.
     *
     * @return closed runs and how each of them ended
     */
    @Transactional(readOnly = true)
    public List<ColonyRunHistoryResponse> findHistory() {
        return runReader.closedRuns().stream().map(this::toHistory).toList();
    }

    /**
     * Returns how many days a run spans, its settlement day included.
     *
     * @return days in a run
     */
    private int runDayCount() {
        return ruleset.runLengthWeeks() * DAYS_IN_WEEK + 1;
    }

    /**
     * Returns the morale and the speed it buys tonight.
     *
     * @param morale today's morale
     * @return the morale readout
     */
    private ColonyMoraleResponse morale(double morale) {
        return new ColonyMoraleResponse(
            morale,
            ruleset.maximumMorale(),
            ruleset.gapClosingRatePercent() * morale / ruleset.maximumMorale()
        );
    }

    /**
     * Returns the steps of the ladder around the town's own.
     *
     * <p>A window rather than the whole ladder, since the ladder has no end.
     *
     * @param current    step the town sits in
     * @param rosterSize roster size frozen on the run, which prices every step in materials
     * @return the window, lowest step first
     */
    private List<ColonyTierResponse> ladder(ColonyTier current, int rosterSize) {
        List<ColonyTierResponse> ladder = new ArrayList<>();
        int firstStep = Math.max(0, current.step() - LADDER_STEPS_BEHIND);

        for (int step = firstStep; step <= current.step() + LADDER_STEPS_AHEAD; step++) {
            ColonyTierState state = ColonyTierState.LOCKED;

            if (step == current.step()) {
                state = ColonyTierState.CURRENT;
            } else if (step < current.step()) {
                state = ColonyTierState.REACHED;
            }

            ladder.add(toTier(ruleset.tierAtStep(step), state, rosterSize));
        }

        return ladder;
    }

    /**
     * Returns the run's ten fights and what each is worth to the colony.
     *
     * <p>Resolved here rather than left to the page, because what a fight pays depends on the category
     * it was drawn at and on the roster frozen on the run — two numbers the page has no business
     * holding. A week with no encounter drawn yet is still listed: a run is ten weeks long, not ten
     * fights long, and the map has a tile for every one of them.
     *
     * @param run         run being read
     * @param currentWeek Monday of the week today falls in
     * @return one entry per week of the run, earliest first
     */
    private List<ColonyWeekResponse> weeks(Run run, LocalDate currentWeek) {
        Map<LocalDate, WeeklyBossEncounter> encounters = new HashMap<>();
        encounterRepository.findAllByRunIdOrderByWeekStartAsc(run.getId())
            .forEach(encounter -> encounters.put(encounter.getWeekStart(), encounter));

        List<ColonyWeekResponse> weeks = new ArrayList<>();

        for (int weekIndex = 1; weekIndex <= ruleset.runLengthWeeks(); weekIndex++) {
            LocalDate weekStart = run.getFirstWeekStart().plusWeeks(weekIndex - 1L);

            weeks.add(toWeek(
                weekIndex,
                weekStart,
                currentWeek,
                encounters.get(weekStart),
                run.getRosterSize()
            ));
        }

        return weeks;
    }

    /**
     * Maps one week of the run to what its fight is worth.
     *
     * <p>An unsettled week is priced at what it <b>would</b> pay rather than at zero, which is what lets
     * the week under way show what is on the table instead of an empty tile.
     *
     * @param weekIndex   week of the run, from one
     * @param weekStart   Monday beginning that week
     * @param currentWeek Monday of the week today falls in
     * @param encounter   that week's fight, {@code null} while none has been drawn
     * @param rosterSize  roster size frozen on the run
     * @return the week, priced
     */
    private ColonyWeekResponse toWeek(
        int weekIndex,
        LocalDate weekStart,
        LocalDate currentWeek,
        WeeklyBossEncounter encounter,
        int rosterSize
    ) {
        if (encounter == null) {
            return new ColonyWeekResponse(
                weekIndex,
                unsettledState(weekStart, currentWeek),
                null,
                0,
                0.0,
                0.0
            );
        }

        BossCategory category = encounter.getBossCatalogEntry().getCategory();

        if (encounter.getFinalizedAt() == null) {
            ColonyWeekOutcomeState state = unsettledState(weekStart, currentWeek);

            // A week still open is quoted at what it *would* pay, which is what lets the tile of the
            // fight under way show what is on the table rather than an empty promise. A week already
            // behind and still open settled nothing, so it is quoted at nothing: anything else would
            // put a figure on the page the model never applied.
            return state == ColonyWeekOutcomeState.SURVIVED
                ? new ColonyWeekResponse(weekIndex, state, category, 0, 0.0, 0.0)
                : priced(weekIndex, state, category, rosterSize);
        }

        if (!encounter.isDefeated()) {
            return new ColonyWeekResponse(
                weekIndex,
                ColonyWeekOutcomeState.SURVIVED,
                category,
                0,
                0.0,
                ruleset.moraleForSurvivingBoss()
            );
        }

        return priced(weekIndex, ColonyWeekOutcomeState.DEFEATED, category, rosterSize);
    }

    /**
     * Prices one week at what beating its boss is worth.
     *
     * @param weekIndex  week of the run, from one
     * @param state      state to report the week in
     * @param category   category the boss was drawn at
     * @param rosterSize roster size frozen on the run
     * @return the week, priced at a win
     */
    private ColonyWeekResponse priced(
        int weekIndex,
        ColonyWeekOutcomeState state,
        BossCategory category,
        int rosterSize
    ) {
        int materials = ruleset.materialsForDefeatedBoss(category, rosterSize);
        double efficiencyGain = ruleset.efficiencyFor(materials, rosterSize) - ruleset.efficiencyFor(0, rosterSize);

        return new ColonyWeekResponse(
            weekIndex,
            state,
            category,
            materials,
            efficiencyGain,
            ruleset.moraleForDefeatedBoss(category)
        );
    }

    /**
     * Returns the state of a week whose fight has not been settled.
     *
     * <p>Three cases, not two. A week still ahead is upcoming and the week today falls in is under
     * way, both obvious; but a week already <b>behind</b> and still unsettled is neither. It happens
     * when a Monday's rollover never fired, and reporting it as under way put three tiles in the
     * "fighting now" state at once. It settled nothing, so the colony was paid nothing for it, and
     * that is exactly what a surviving boss means here.
     *
     * @param weekStart   Monday beginning the week
     * @param currentWeek Monday of the week today falls in
     * @return the state of that unsettled week
     */
    private ColonyWeekOutcomeState unsettledState(LocalDate weekStart, LocalDate currentWeek) {
        if (weekStart.isAfter(currentWeek)) {
            return ColonyWeekOutcomeState.UPCOMING;
        }

        return weekStart.isBefore(currentWeek)
            ? ColonyWeekOutcomeState.SURVIVED
            : ColonyWeekOutcomeState.CURRENT;
    }

    /**
     * Maps one step of the ladder to its response.
     *
     * @param tier       step to map
     * @param state      where it stands relative to the town
     * @param rosterSize roster size frozen on the run, which prices the step in materials
     * @return the step, exposed
     */
    private ColonyTierResponse toTier(ColonyTier tier, ColonyTierState state, int rosterSize) {
        return new ColonyTierResponse(
            tier.name(),
            tier.level(),
            tier.threshold(),
            ruleset.materialsForEfficiency(tier.threshold(), rosterSize),
            state
        );
    }

    /**
     * Returns the days the town changed name, for the curve to be read against.
     *
     * @param snapshots the run's snapshots, oldest day first
     * @param run       run being read
     * @return milestones, oldest first
     */
    private List<ColonyMilestoneResponse> milestones(List<ColonyDailySnapshot> snapshots, Run run) {
        List<ColonyMilestoneResponse> milestones = new ArrayList<>();
        int previousStep = -1;

        for (ColonyDailySnapshot snapshot : snapshots) {
            ColonyTier tier = ruleset.tierFor(snapshot.getEfficiency().doubleValue());

            if (previousStep >= 0 && tier.step() > previousStep) {
                milestones.add(new ColonyMilestoneResponse(
                    tier.name(),
                    tier.level(),
                    snapshot.getDay(),
                    runReader.runDayOf(run, snapshot.getDay()),
                    tier.threshold()
                ));
            }

            previousStep = tier.step();
        }

        return milestones;
    }

    /**
     * Maps one snapshot to a curve point.
     *
     * @param run      run the day belongs to
     * @param snapshot day to map
     * @return curve point
     */
    private ColonyTrajectoryPointResponse toPoint(Run run, ColonyDailySnapshot snapshot) {
        double foodStock = snapshot.getFoodStock().doubleValue();
        double efficiency = snapshot.getEfficiency().doubleValue();

        return new ColonyTrajectoryPointResponse(
            snapshot.getDay(),
            runReader.runDayOf(run, snapshot.getDay()),
            rounded(snapshot.getPopulation().doubleValue()),
            rounded(engine.feedablePopulation(foodStock, efficiency)),
            efficiency,
            snapshot.getMaterials(),
            foodStock,
            snapshot.getMorale().doubleValue(),
            snapshot.getPresenceCount()
        );
    }

    /**
     * Maps one closed run to its history entry.
     *
     * @param run closed run
     * @return history entry
     */
    private ColonyRunHistoryResponse toHistory(Run run) {
        List<ColonyDailySnapshot> snapshots = runReader.settledSnapshotsOf(run);

        int finalPopulation = snapshots.isEmpty()
            ? 0
            : rounded(snapshots.getLast().getPopulation().doubleValue());

        int peak = snapshots.stream()
            .mapToInt(snapshot -> rounded(snapshot.getPopulation().doubleValue()))
            .max()
            .orElse(0);

        int average = rounded(snapshots.stream()
            .mapToDouble(snapshot -> snapshot.getPopulation().doubleValue())
            .average()
            .orElse(0.0));

        double efficiency = snapshots.isEmpty()
            ? ruleset.efficiencyFor(0, run.getRosterSize())
            : snapshots.getLast().getEfficiency().doubleValue();
        int materials = snapshots.isEmpty() ? 0 : snapshots.getLast().getMaterials();

        return new ColonyRunHistoryResponse(
            run.getNumber(),
            run.getFirstWeekStart(),
            run.settlementDay(),
            finalPopulation,
            peak,
            average,
            efficiency,
            materials,
            toTier(ruleset.tierFor(efficiency), ColonyTierState.REACHED, run.getRosterSize()),
            defeatedBosses(run),
            ruleset.runLengthWeeks()
        );
    }

    /**
     * Counts the bosses a run put down.
     *
     * @param run run to read
     * @return defeated bosses
     */
    private int defeatedBosses(Run run) {
        return (int) encounterRepository
            .findAllByRunIdAndFinalizedAtIsNotNullOrderByWeekStartAsc(run.getId())
            .stream()
            .filter(WeeklyBossEncounter::isDefeated)
            .count();
    }

    /**
     * Rounds one computed value to the inhabitant.
     *
     * @param value computed value
     * @return nearest whole number
     */
    private static int rounded(double value) {
        return (int) Math.round(value);
    }
}
