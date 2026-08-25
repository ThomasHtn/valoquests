package io.github.thomashtn.valoquests.colony.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.boss.repository.WeeklyBossEncounterRepository;
import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import io.github.thomashtn.valoquests.colony.dto.ColonyBuildingResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyGaugeResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyMilestoneResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyNextTierResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyRunHistoryResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyTrajectoryPointResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyTrajectoryResponse;
import io.github.thomashtn.valoquests.colony.entity.ColonyDailySnapshot;
import io.github.thomashtn.valoquests.colony.model.ColonyBuildingTier;
import io.github.thomashtn.valoquests.colony.model.ColonyGauge;
import io.github.thomashtn.valoquests.colony.repository.ColonyDailySnapshotRepository;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.run.service.RunService;
import io.github.thomashtn.valoquests.week.WeekCalendar;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads the colony off its snapshots.
 *
 * <p>Computes nothing the engine has not already decided: health, the daily loss and the equilibrium
 * all come back through {@link ColonyReplayEngine}, so the page and the replay can never disagree on a
 * formula.
 */
@Service
public class ColonyQueryService {

    /**
     * Divisor turning a ratio into a percentage.
     */
    private static final double PERCENT_SCALE = 100.0;

    /**
     * Service resolving the run in progress.
     */
    private final RunService runService;

    /**
     * Service rebuilding a run that has no snapshot yet.
     */
    private final ColonyReplayService replayService;

    /**
     * Snapshot repository.
     */
    private final ColonyDailySnapshotRepository snapshotRepository;

    /**
     * Repository counting the bosses a run put down.
     */
    private final WeeklyBossEncounterRepository encounterRepository;

    /**
     * Calibration the thresholds and limits are read from.
     */
    private final ColonyRuleset ruleset;

    /**
     * Engine supplying the formulas the page displays.
     */
    private final ColonyReplayEngine engine;

    /**
     * Calendar resolving the current week.
     */
    private final WeekCalendar weekCalendar;

    /**
     * Creates the colony query service.
     *
     * @param runService          run service
     * @param replayService       colony replay service
     * @param snapshotRepository  colony daily snapshot repository
     * @param encounterRepository weekly boss encounter repository
     * @param ruleset             colony ruleset
     * @param engine              colony replay engine
     * @param weekCalendar        week calendar
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public ColonyQueryService(
        RunService runService,
        ColonyReplayService replayService,
        ColonyDailySnapshotRepository snapshotRepository,
        WeeklyBossEncounterRepository encounterRepository,
        ColonyRuleset ruleset,
        ColonyReplayEngine engine,
        WeekCalendar weekCalendar
    ) {
        this.runService = runService;
        this.replayService = replayService;
        this.snapshotRepository = snapshotRepository;
        this.encounterRepository = encounterRepository;
        this.ruleset = ruleset;
        this.engine = engine;
        this.weekCalendar = weekCalendar;
    }

    /**
     * Returns the colony as it stands today.
     *
     * @return today's colony
     */
    @Transactional
    public ColonyResponse findCurrent() {
        Run run = currentRun();
        List<ColonyDailySnapshot> snapshots = snapshotsOf(run);

        ColonyDailySnapshot today = snapshots.getLast();
        ColonyDailySnapshot yesterday = snapshots.size() > 1
            ? snapshots.get(snapshots.size() - 2)
            : today;

        double food = today.getFood().doubleValue();
        double energy = today.getEnergy().doubleValue();
        double health = engine.health(food, energy);
        double loss = engine.dailyLoss(
            yesterday.getPopulation().doubleValue(),
            yesterday.getCapacity()
        );

        return new ColonyResponse(
            run.getNumber(),
            (int) ChronoUnit.DAYS.between(run.getFirstWeekStart(), today.getDay()) + 1,
            runDayCount(),
            (int) ChronoUnit.WEEKS.between(
                run.getFirstWeekStart(),
                weekCalendar.weekStartOf(today.getDay())
            ) + 1,
            ruleset.runLengthWeeks(),
            today.getDay(),
            new ColonyGaugeResponse(food, today.getFoodGain().doubleValue(), loss),
            new ColonyGaugeResponse(energy, today.getEnergyGain().doubleValue(), loss),
            health * PERCENT_SCALE,
            health < ruleset.alertHealthThreshold(),
            rounded(today.getPopulation().doubleValue()),
            rounded(engine.targetPopulation(today.getCapacity(), health)),
            rounded(today.getPopulation().doubleValue())
                - rounded(yesterday.getPopulation().doubleValue()),
            rounded(today.getCapacity() * ruleset.growthRatePercent() / PERCENT_SCALE),
            today.getCapacity(),
            ruleset.maximumCapacity(),
            today.getMaterials(),
            buildings(snapshots, run),
            nextTier(today.getMaterials()),
            limitingGauge(today),
            equilibriumPercentage(today),
            defeatedBosses(run),
            ruleset.runLengthWeeks(),
            ruleset.materialsPerDefeatedBoss()
        );
    }

    /**
     * Returns the population curve of the run in progress.
     *
     * @return the run's curve, with its peak, its average and its building milestones
     */
    @Transactional
    public ColonyTrajectoryResponse findTrajectory() {
        Run run = currentRun();
        List<ColonyDailySnapshot> snapshots = snapshotsOf(run);

        ColonyDailySnapshot peak = snapshots.stream()
            .max(java.util.Comparator.comparing(ColonyDailySnapshot::getPopulation))
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
        return runService.closedRuns().stream().map(this::toHistory).toList();
    }

    /**
     * Resolves the run in progress, opening one when the deployment has not seen a rollover yet.
     *
     * <p>Mirrors how the boss endpoint already draws a week's fight lazily: the campaign should not
     * depend on a scheduled job having fired for a page to render.
     *
     * @return the run in progress
     */
    private Run currentRun() {
        return runService.ensureRunFor(weekCalendar.currentWeekStart());
    }

    /**
     * Returns a run's snapshots, replaying it once when it has none yet.
     *
     * @param run run to read
     * @return the run's snapshots, oldest day first, never empty
     */
    private List<ColonyDailySnapshot> snapshotsOf(Run run) {
        List<ColonyDailySnapshot> snapshots =
            snapshotRepository.findAllByRunIdOrderByDayAsc(run.getId());

        if (!snapshots.isEmpty()) {
            return snapshots;
        }

        replayService.replay(run);

        return snapshotRepository.findAllByRunIdOrderByDayAsc(run.getId());
    }

    /**
     * Returns how many days a run spans, its settlement day included.
     *
     * @return days in a run
     */
    private int runDayCount() {
        return ruleset.runLengthWeeks() * 7 + 1;
    }

    /**
     * Returns which gauge is currently setting the equilibrium population.
     *
     * @param today today's snapshot
     * @return the gauge fed the least
     */
    private ColonyGauge limitingGauge(ColonyDailySnapshot today) {
        return today.getFoodGain().compareTo(today.getEnergyGain()) <= 0
            ? ColonyGauge.FOOD
            : ColonyGauge.ENERGY;
    }

    /**
     * Returns the share of capacity the colony plateaus at while today's inputs hold.
     *
     * <p>The model's fixed point, {@code min(Food gain, Energy gain) / 14}. It is what makes the weak
     * link literal, and what gives the feature its anti-farming guarantee without a dedicated rule.
     *
     * @param today today's snapshot
     * @return equilibrium as a percentage of capacity, capped at one hundred
     */
    private double equilibriumPercentage(ColonyDailySnapshot today) {
        double smallestGain = Math.min(
            today.getFoodGain().doubleValue(),
            today.getEnergyGain().doubleValue()
        );

        return Math.min(
            PERCENT_SCALE,
            smallestGain / ruleset.dailyLossCoefficient() * PERCENT_SCALE
        );
    }

    /**
     * Returns every building tier, with the day the run reached it.
     *
     * @param snapshots the run's snapshots, oldest day first
     * @param run       run being read
     * @return every tier, erected or not
     */
    private List<ColonyBuildingResponse> buildings(List<ColonyDailySnapshot> snapshots, Run run) {
        int materials = snapshots.getLast().getMaterials();
        List<ColonyBuildingResponse> buildings = new ArrayList<>();

        for (ColonyBuildingTier tier : ruleset.buildings()) {
            Integer erectedOn = erectedOnRunDay(snapshots, run, tier);

            buildings.add(new ColonyBuildingResponse(
                tier.building(),
                tier.materialsThreshold(),
                tier.capacity(),
                materials >= tier.materialsThreshold(),
                erectedOn
            ));
        }

        return buildings;
    }

    /**
     * Returns the day of the run a tier went up on.
     *
     * @param snapshots the run's snapshots, oldest day first
     * @param run       run being read
     * @param tier      tier to place
     * @return day of the run, or {@code null} while the tier is not up
     */
    private Integer erectedOnRunDay(
        List<ColonyDailySnapshot> snapshots,
        Run run,
        ColonyBuildingTier tier
    ) {
        return snapshots.stream()
            .filter(snapshot -> snapshot.getMaterials() >= tier.materialsThreshold())
            .findFirst()
            .map(snapshot -> runDayOf(run, snapshot.getDay()))
            .orElse(null);
    }

    /**
     * Returns the tier a run is working towards.
     *
     * @param materials cumulative materials
     * @return the next tier, or {@code null} once the last one is up
     */
    private ColonyNextTierResponse nextTier(int materials) {
        return ruleset.buildings().stream()
            .filter(tier -> materials < tier.materialsThreshold())
            .findFirst()
            .map(tier -> new ColonyNextTierResponse(
                tier.building(),
                tier.materialsThreshold(),
                tier.capacity(),
                tier.materialsThreshold() - materials,
                materials * PERCENT_SCALE / tier.materialsThreshold()
            ))
            .orElse(null);
    }

    /**
     * Returns the days buildings went up, for the curve to be read against.
     *
     * @param snapshots the run's snapshots, oldest day first
     * @param run       run being read
     * @return milestones, cheapest tier first, the free starting camp excluded
     */
    private List<ColonyMilestoneResponse> milestones(
        List<ColonyDailySnapshot> snapshots,
        Run run
    ) {
        List<ColonyMilestoneResponse> milestones = new ArrayList<>();

        for (ColonyBuildingTier tier : ruleset.buildings()) {
            if (tier.materialsThreshold() == 0) {
                continue;
            }

            snapshots.stream()
                .filter(snapshot -> snapshot.getMaterials() >= tier.materialsThreshold())
                .findFirst()
                .ifPresent(snapshot -> milestones.add(new ColonyMilestoneResponse(
                    tier.building(),
                    snapshot.getDay(),
                    runDayOf(run, snapshot.getDay()),
                    tier.capacity()
                )));
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
        return new ColonyTrajectoryPointResponse(
            snapshot.getDay(),
            runDayOf(run, snapshot.getDay()),
            rounded(snapshot.getPopulation().doubleValue()),
            snapshot.getCapacity(),
            snapshot.getMaterials(),
            snapshot.getFood().doubleValue(),
            snapshot.getEnergy().doubleValue(),
            snapshot.getActivePlayerCount()
        );
    }

    /**
     * Maps one closed run to its history entry.
     *
     * @param run closed run
     * @return history entry
     */
    private ColonyRunHistoryResponse toHistory(Run run) {
        List<ColonyDailySnapshot> snapshots =
            snapshotRepository.findAllByRunIdOrderByDayAsc(run.getId());

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

        int materials = snapshots.isEmpty() ? 0 : snapshots.getLast().getMaterials();
        int erected = (int) ruleset.buildings().stream()
            .filter(tier -> materials >= tier.materialsThreshold())
            .count();

        return new ColonyRunHistoryResponse(
            run.getNumber(),
            run.getFirstWeekStart(),
            run.settlementDay(),
            finalPopulation,
            finalPopulation * PERCENT_SCALE / ruleset.maximumCapacity(),
            peak,
            average,
            erected,
            ruleset.buildings().size(),
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
            .filter(encounter -> encounter.isDefeated())
            .count();
    }

    /**
     * Places a calendar day inside its run, counting from one.
     *
     * @param run run the day belongs to
     * @param day day to place
     * @return the day's one-based position in the run
     */
    private int runDayOf(Run run, LocalDate day) {
        return (int) ChronoUnit.DAYS.between(run.getFirstWeekStart(), day) + 1;
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
