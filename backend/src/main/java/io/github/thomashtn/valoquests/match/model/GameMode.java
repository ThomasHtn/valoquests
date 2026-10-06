package io.github.thomashtn.valoquests.match.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Defines the supported game modes and the Henrik identifiers each one answers to.
 *
 * <p>{@code importEligible} is hard-coded on purpose: changing the imported set would leave invisible
 * holes in seasons already marked fully synchronized.
 */
public enum GameMode {

    /**
     * Ranked queue, the reference mode of the game.
     */
    COMPETITIVE(true, true, "competitive"),
    UNRATED(true, true, "unrated"),
    SWIFTPLAY(true, true, "swiftplay"),

    /**
     * New-map queue, kept apart from {@link #UNRATED} and {@link #SWIFTPLAY} because Riot changes its ruleset.
     */
    NEW_MAP(true, false, "newmap"),

    /**
     * Short round-based mode.
     */
    SPIKE_RUSH(true, true, "spikerush"),
    DEATHMATCH(false, true, "deathmatch"),
    TEAM_DEATHMATCH(false, true, "teamdeathmatch", "hurm"),
    ESCALATION(false, true, "escalation", "ggteam"),

    /**
     * Compact 2v2 gunplay mode, distinct from {@link #ESCALATION}, also catching unknown {@code skirmish*} variants.
     */
    SKIRMISH(true, true, "skirmish2v2", "skirmish"),

    /**
     * Team-based ranked queue.
     */
    PREMIER(true, true, "premier"),

    /**
     * Private match; a queue, not a ruleset, so its {@code mode_type} must never decide the mode.
     */
    CUSTOM(true, false, "custom", "customgame"),

    /**
     * Fallback for an unrecognized queue, imported with its raw slug so a reclassification needs no re-import.
     */
    OTHER(false, true);

    /**
     * Identifiers resolving to a Skirmish variant even when the exact spelling is unknown.
     */
    private static final String SKIRMISH_PREFIX = "skirmish";

    /**
     * Allow-list of the modes {@code DefaultScoringRuleset#matchDamage} prices, so {@link #OTHER} never counts.
     */
    private static final Set<GameMode> SCORED_MODES = EnumSet.of(
        COMPETITIVE,
        UNRATED,
        SPIKE_RUSH,
        DEATHMATCH,
        TEAM_DEATHMATCH,
        SKIRMISH,
        PREMIER,
        SWIFTPLAY,
        ESCALATION
    );

    /**
     * Whether the mode is played in rounds, so a round score and a headshot rate mean something.
     */
    private final boolean roundBased;

    /**
     * Whether synchronization stores matches of this mode.
     */
    private final boolean importEligible;

    /**
     * Names the Henrik API uses for this mode, lower-cased.
     */
    private final Set<String> aliases;

    GameMode(boolean roundBased, boolean importEligible, String... aliases) {
        this.roundBased = roundBased;
        this.importEligible = importEligible;
        this.aliases = Set.of(aliases);
    }

    /**
     * Resolves a raw Henrik queue identifier to a game mode.
     *
     * <p>Returns empty rather than {@link #OTHER} so callers can try the next identifier.
     *
     * @param rawIdentifier raw {@code queue.id}, {@code queue.name} or {@code queue.mode_type} value
     * @return the matching game mode, or empty when the identifier is blank or unknown
     */
    public static Optional<GameMode> fromIdentifier(String rawIdentifier) {
        if (rawIdentifier == null || rawIdentifier.isBlank()) {
            return Optional.empty();
        }

        String normalized = normalize(rawIdentifier);

        return Arrays.stream(values())
            .filter(gameMode -> gameMode.aliases.contains(normalized))
            .findFirst()
            .or(() -> normalized.startsWith(SKIRMISH_PREFIX)
                ? Optional.of(SKIRMISH)
                : Optional.empty());
    }

    /**
     * Indicates whether this mode plays scored rounds, and therefore supports per-round averages.
     *
     * @return {@code true} when ACS and ADR are meaningful for this mode
     */
    public boolean isRoundBased() {
        return roundBased;
    }

    /**
     * Indicates whether the competition counts matches of this mode at all.
     *
     * <p>Unscored matches stay stored and shown in history but feed neither campaign nor ranking.
     *
     * @return {@code true} when a match of this mode can count
     */
    public boolean isScored() {
        return SCORED_MODES.contains(this);
    }

    /**
     * Indicates whether synchronization stores matches of this mode.
     *
     * @return {@code true} when matches of this mode must be imported
     */
    public boolean isImportEligible() {
        return importEligible;
    }

    /**
     * Lower-cases a mode name and strips its separators, so aliases compare loosely.
     */
    private static String normalize(String value) {
        return value
            .toLowerCase(Locale.ROOT)
            .replace("_", "")
            .replace("-", "")
            .replace(" ", "");
    }
}
