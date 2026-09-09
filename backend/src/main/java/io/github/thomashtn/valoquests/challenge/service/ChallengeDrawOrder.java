package io.github.thomashtn.valoquests.challenge.service;

import io.github.thomashtn.valoquests.challenge.entity.Challenge;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Deterministic ordering of challenge candidates for one draw day.
 *
 * <p>The order only depends on the day, the challenge and an optional salt, so the same week
 * produces the same candidate order across application restarts. The salt separates a manual
 * redraw from the scheduled draw: it goes into the same seed rather than beside it, because it is
 * shared by every candidate and only the avalanche turns it into a different order.</p>
 */
final class ChallengeDrawOrder {

    /**
     * Salt of the scheduled draw, which must stay reproducible across restarts.
     */
    static final long UNSALTED_DRAW = 0L;

    /**
     * Odd 64-bit constant separating consecutive days before diffusion (golden-ratio derived).
     */
    private static final long DAY_SEED_MULTIPLIER = 0x9E3779B97F4A7C15L;

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
     * Not instantiable: static helpers only.
     */
    private ChallengeDrawOrder() {
    }

    /**
     * Computes the ordering value of one candidate for one draw.
     *
     * @param drawDay   day identifying the draw
     * @param challenge challenge candidate
     * @param drawSalt  salt separating a manual redraw from the scheduled draw
     * @return deterministic ordering value
     */
    static long of(LocalDate drawDay, Challenge challenge, long drawSalt) {
        long challengeSeed = Objects.hash(challenge.getId(), challenge.getCode());

        return avalanche(drawDay.toEpochDay() * DAY_SEED_MULTIPLIER + challengeSeed + drawSalt);
    }

    /**
     * Spreads a seed over the whole {@code long} range so neighbouring seeds order unrelatedly.
     *
     * <p>SplitMix64 finalizer: a bijection, so two distinct seeds keep distinct ordering values.</p>
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
}
