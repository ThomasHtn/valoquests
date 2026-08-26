package io.github.thomashtn.valoquests.colony.service;

import io.github.thomashtn.valoquests.colony.ColonyRuleset;
import io.github.thomashtn.valoquests.colony.dto.ColonyPresencePlayerResponse;
import io.github.thomashtn.valoquests.colony.dto.ColonyPresenceResponse;
import io.github.thomashtn.valoquests.colony.model.ColonyPresenceState;
import io.github.thomashtn.valoquests.player.entity.Player;
import io.github.thomashtn.valoquests.player.repository.PlayerRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Names the day's turnout, player by player.
 *
 * <p>The count and the multiplier are the replay's, so the page can never show a multiplier the model
 * did not apply. The list, on the other hand, is the roster <b>as it currently stands</b>: only its size
 * is frozen on the run, not its membership, so a roster edited mid-run shows more or fewer pips than the
 * denominator. That discrepancy is real and worth seeing — it is exactly what the backoffice exists to
 * resolve.
 */
@Service
@Transactional(readOnly = true)
public class ColonyPresenceReader {

    /**
     * Repository listing the roster the readout names.
     */
    private final PlayerRepository playerRepository;

    /**
     * Reader supplying what each player brought to a day.
     */
    private final ColonyActivityReader activityReader;

    /**
     * Ruleset supplying the threshold a day must clear.
     */
    private final ColonyRuleset ruleset;

    /**
     * Engine supplying the multiplier formula the replay itself uses.
     */
    private final ColonyReplayEngine engine;

    /**
     * Creates the presence reader.
     *
     * @param playerRepository player repository
     * @param activityReader   colony activity reader
     * @param ruleset          colony ruleset
     * @param engine           colony replay engine
     */
    public ColonyPresenceReader(
        PlayerRepository playerRepository,
        ColonyActivityReader activityReader,
        ColonyRuleset ruleset,
        ColonyReplayEngine engine
    ) {
        this.playerRepository = playerRepository;
        this.activityReader = activityReader;
        this.ruleset = ruleset;
        this.engine = engine;
    }

    /**
     * Returns one day's turnout, named.
     *
     * @param day           day to read
     * @param presenceCount players the replay counted that day
     * @param rosterSize    roster size frozen on the run
     * @return the day's turnout
     */
    public ColonyPresenceResponse read(LocalDate day, int presenceCount, int rosterSize) {
        Map<Long, Integer> rawDamageByPlayer = activityReader.readRawDamageByPlayer(day);
        List<ColonyPresencePlayerResponse> players = new ArrayList<>();

        for (Player player : playerRepository.findAllByStatusOrderByIdAsc(Player.COMPETITIVE_STATUS)) {
            int rawDamage = rawDamageByPlayer.getOrDefault(player.getId(), 0);

            players.add(new ColonyPresencePlayerResponse(
                player.getId(),
                player.getDisplayName(),
                stateOf(rawDamage),
                rawDamage
            ));
        }

        return new ColonyPresenceResponse(
            presenceCount,
            rosterSize,
            engine.presenceMultiplier(presenceCount, rosterSize),
            ruleset.presenceDamageThreshold(),
            players
        );
    }

    /**
     * Returns how far into the day one player got.
     *
     * <p>Three states rather than two: an evening of two deathmatches brings food in and still does not
     * count towards the multiplier, and drawing that as "did not play" would be false.
     *
     * @param rawDamage raw damage the player brought in that day
     * @return their turnout state
     */
    private ColonyPresenceState stateOf(int rawDamage) {
        if (rawDamage >= ruleset.presenceDamageThreshold()) {
            return ColonyPresenceState.FULL;
        }

        return rawDamage > 0 ? ColonyPresenceState.PARTIAL : ColonyPresenceState.NONE;
    }
}
