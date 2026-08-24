package io.github.thomashtn.valoquests.match.model;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Defines the supported game mode values.
 *
 * <p>Each constant owns the Henrik/Riot identifiers it answers to, so adding a game mode released by
 * Riot is a single-constant change. Identifiers are matched in their normalized form: lower case,
 * without {@code _}, {@code -} or spaces. Riot exposes the same mode under several spellings
 * depending on the field ({@code queue.id}, {@code queue.name}, {@code queue.mode_type}), hence the
 * multiple aliases per constant.
 *
 * <p>Only identifiers actually observed in Henrik responses are declared. Riot's internal asset
 * folder names, such as {@code QuickBomb} for Spike Rush, are deliberately absent: Henrik reports
 * the asset display name, never its path, so those would be aliases that can never match.
 *
 * <p>{@code roundBased} states whether a mode plays scored rounds. Per-round averages such as ACS and
 * ADR are only meaningful for those modes; for the others the round count is either absent or
 * unrelated to scoring.
 *
 * <p>{@code importEligible} states whether synchronization stores matches of this mode. It is
 * declared here rather than made configurable on purpose: the set of imported modes defines what
 * "this season is fully synchronized" means. A season walked and marked complete under one set is
 * not complete under another, and nothing records which set was in force, so widening a
 * configurable filter would leave permanent invisible holes in every already-complete season.
 */
public enum GameMode {

    COMPETITIVE(true, true, "competitive"),
    UNRATED(true, true, "unrated"),
    SWIFTPLAY(true, false, "swiftplay"),

    /**
     * Dedicated queue used to introduce a newly released map.
     *
     * <p>Kept separate from {@link #UNRATED} and {@link #SWIFTPLAY} on purpose: Riot has already
     * switched this queue's ruleset once, and folding it into either mode would silently change the
     * matches counted by challenges filtered on that mode.
     */
    NEW_MAP(true, false, "newmap"),

    SPIKE_RUSH(true, true, "spikerush"),
    DEATHMATCH(false, true, "deathmatch"),
    TEAM_DEATHMATCH(false, true, "teamdeathmatch", "hurm"),
    ESCALATION(false, false, "escalation", "ggteam"),

    /**
     * Compact 2v2 gunplay mode, distinct from {@link #ESCALATION}.
     *
     * <p>Riot ships Skirmish as its own game mode asset and declines it into variants such as
     * Skirmish: Ascension, which is why unknown {@code skirmish*} identifiers resolve here.
     */
    SKIRMISH(true, true, "skirmish2v2", "skirmish"),

    PREMIER(true, true, "premier"),

    /**
     * Private match created by the players themselves.
     *
     * <p>Henrik reports it either as the {@code custom} slug or as an empty slug carrying the
     * {@code Custom Game} display name, hence both aliases. A custom match is a queue, not a
     * ruleset: it can be played with any ruleset, so its {@code mode_type} says Skirmish or Team
     * Deathmatch and must never be what decides the mode.
     */
    CUSTOM(true, false, "custom", "customgame"),

    /**
     * Fallback for a queue this application does not recognize yet.
     *
     * <p>Imported despite not being one of the modes the tracker follows: an unrecognized queue is
     * precisely the case where eligibility cannot be decided yet. Henrik lags behind Riot releases,
     * so a mode that matters may surface here first. The raw slug is preserved in
     * {@code valorant_match.queue_id}, which makes a later reclassification a plain data migration
     * instead of a full history re-import.
     */
    OTHER(false, true);

    /**
     * Identifiers resolving to a Skirmish variant even when the exact spelling is unknown.
     */
    private static final String SKIRMISH_PREFIX = "skirmish";

    /**
     * Modes the competition counts, which is exactly the set {@code DefaultScoringRuleset#matchDamage}
     * prices.
     *
     * <p>Declared as the modes that <em>do</em> count rather than as the ones that do not, so a mode
     * released by Riot and not yet priced defaults to being ignored instead of silently entering the
     * competition at zero damage. {@link #OTHER} is the case that made this necessary: an unrecognized
     * queue is imported on purpose, so a later reclassification is a data migration rather than a full
     * re-import — but while it sits there unrecognized it is worth no damage, and it used to count as a
     * day played and to progress any challenge that filtered on no particular mode. A match nobody can
     * price must not be able to move the regularity bonus or a volume target.
     *
     * <p>Declared after the constants because an {@link EnumSet} of this enum cannot be built before
     * they exist.
     */
    private static final Set<GameMode> SCORED_MODES = EnumSet.of(
        COMPETITIVE,
        UNRATED,
        SPIKE_RUSH,
        DEATHMATCH,
        TEAM_DEATHMATCH,
        SKIRMISH,
        PREMIER
    );

    private final boolean roundBased;

    private final boolean importEligible;

    private final Set<String> aliases;

    GameMode(boolean roundBased, boolean importEligible, String... aliases) {
        this.roundBased = roundBased;
        this.importEligible = importEligible;
        this.aliases = Set.of(aliases);
    }

    /**
     * Resolves a raw Henrik queue identifier to a game mode.
     *
     * <p>Deliberately returns an empty result rather than {@link #OTHER} so callers can try the next
     * identifier Henrik exposes before giving up.
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
     * <p>A mode that is not scored carries no damage, no day played and no challenge progress — see
     * {@link #SCORED_MODES}. Its matches are still stored and still shown in a player's history: they
     * were played, they are simply not part of the weekly fight.
     *
     * @return {@code true} when a match of this mode can count
     */
    public boolean isScored() {
        return SCORED_MODES.contains(this);
    }

    /**
     * Indicates whether synchronization stores matches of this mode.
     *
     * <p>A mode that is not eligible is skipped before any row is created, so it never reaches the
     * match tables and never counts towards challenge progression.
     *
     * @return {@code true} when matches of this mode must be imported
     */
    public boolean isImportEligible() {
        return importEligible;
    }

    private static String normalize(String value) {
        return value
            .toLowerCase(Locale.ROOT)
            .replace("_", "")
            .replace("-", "")
            .replace(" ", "");
    }
}
