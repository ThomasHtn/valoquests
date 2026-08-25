package io.github.thomashtn.valoquests.colony.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.github.thomashtn.valoquests.colony.entity.ColonyDailySnapshot;
import io.github.thomashtn.valoquests.colony.model.ColonyDailyInput;
import io.github.thomashtn.valoquests.colony.model.ColonyDayState;
import io.github.thomashtn.valoquests.colony.repository.ColonyDailySnapshotRepository;
import io.github.thomashtn.valoquests.run.entity.Run;
import io.github.thomashtn.valoquests.run.service.RunService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rebuilds the run in progress and rewrites its snapshots.
 *
 * <p>Never mutates a state incrementally. The run is replayed from its first day every single time,
 * which is what makes this safe to call from a nightly tick, from the end of every synchronization and
 * from an admin endpoint, in any order and any number of times.
 *
 * <p>Only the run in progress is ever replayed. A closed run is frozen: its score and its secondary
 * figures never move again, so a later rebalancing of the ruleset cannot rewrite history.
 */
@Service
public class ColonyReplayService {

    /**
     * Application logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ColonyReplayService.class);

    /**
     * Decimals the {@code NUMERIC} columns keep.
     *
     * <p>Display precision only. The replay always starts from the initial state and never reads a
     * stored value back, so rounding here cannot compound across days.
     */
    private static final int STORED_SCALE = 3;

    /**
     * Service resolving the run in progress.
     */
    private final RunService runService;

    /**
     * Assembler turning the run into the days the engine consumes.
     */
    private final ColonyRunInputAssembler inputAssembler;

    /**
     * Engine computing the colony day by day.
     */
    private final ColonyReplayEngine engine;

    /**
     * Snapshot repository.
     */
    private final ColonyDailySnapshotRepository snapshotRepository;

    /**
     * Creates the replay service.
     *
     * @param runService         run service
     * @param inputAssembler     run input assembler
     * @param engine             replay engine
     * @param snapshotRepository colony daily snapshot repository
     */
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "The injected collaborator is managed by Spring and cannot be defensively copied."
    )
    public ColonyReplayService(
        RunService runService,
        ColonyRunInputAssembler inputAssembler,
        ColonyReplayEngine engine,
        ColonyDailySnapshotRepository snapshotRepository
    ) {
        this.runService = runService;
        this.inputAssembler = inputAssembler;
        this.engine = engine;
        this.snapshotRepository = snapshotRepository;
    }

    /**
     * Replays the run in progress, if there is one.
     *
     * @return the states written, empty while no run has been opened
     */
    @Transactional
    public List<ColonyDayState> replayCurrentRun() {
        Optional<Run> currentRun = runService.currentRun();

        if (currentRun.isEmpty()) {
            LOGGER.debug("No run is in progress: the colony has nothing to replay yet.");
            return List.of();
        }

        return replay(currentRun.orElseThrow());
    }

    /**
     * Replays one run and rewrites its snapshots.
     *
     * @param run run to replay
     * @return the states written, oldest day first
     */
    @Transactional
    public List<ColonyDayState> replay(Run run) {
        List<ColonyDailyInput> days = inputAssembler.assemble(run);
        List<ColonyDayState> states = engine.replay(days, run.getRosterSize());

        // Deleted and written again rather than updated in place: a run whose length or calendar moved
        // would otherwise leave orphan days behind, and the whole point of the replay is that the rows
        // it produces depend on nothing but the inputs it just read.
        snapshotRepository.deleteAllByRunId(run.getId());
        snapshotRepository.flush();
        snapshotRepository.saveAll(states.stream().map(state -> toSnapshot(run, state)).toList());

        LOGGER.info(
            "Colony replayed for run {}: {} day(s), population {} of a {} capacity.",
            run.getNumber(),
            states.size(),
            states.isEmpty() ? 0 : Math.round(states.getLast().population()),
            states.isEmpty() ? 0 : states.getLast().capacity()
        );

        return states;
    }

    /**
     * Maps one computed day to the row that stores it.
     *
     * @param run   run the day belongs to
     * @param state computed day
     * @return snapshot ready to persist
     */
    private ColonyDailySnapshot toSnapshot(Run run, ColonyDayState state) {
        ColonyDailySnapshot snapshot = new ColonyDailySnapshot();
        snapshot.setRun(run);
        snapshot.setDay(state.day());
        snapshot.setFood(stored(state.food()));
        snapshot.setEnergy(stored(state.energy()));
        snapshot.setMaterials(state.materials());
        snapshot.setPopulation(stored(state.population()));
        snapshot.setCapacity(state.capacity());
        snapshot.setActivePlayerCount(state.activePlayerCount());
        snapshot.setFoodGain(stored(state.foodGain()));
        snapshot.setEnergyGain(stored(state.energyGain()));

        return snapshot;
    }

    /**
     * Rounds one computed value to what the column keeps.
     *
     * @param value computed value
     * @return value at the stored scale
     */
    private static BigDecimal stored(double value) {
        return BigDecimal.valueOf(value).setScale(STORED_SCALE, RoundingMode.HALF_UP);
    }
}
