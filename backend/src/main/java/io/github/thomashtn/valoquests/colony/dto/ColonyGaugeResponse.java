package io.github.thomashtn.valoquests.colony.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes one gauge, what moved it today, and the level it is heading for.
 *
 * <p>{@code equilibrium} exists because the raw value of a gauge is unreadable on its own. The gauge
 * that limits the colony settles far below the health it produces, so a squad comfortably holding half
 * of its capacity still sees that bar sitting around a third. Shown against the level it settles at,
 * the same bar reads "on the mark" instead of "starving".
 *
 * <p>What the day <i>cost</i> is deliberately not here: it is charged when the day opens, so it is
 * already inside {@code value}, and the figure worth acting on is the next one. See
 * {@link ColonyUpkeepResponse}.
 *
 * @param value       today's value, in {@code [0, 100]}
 * @param gain        what the day has brought in so far, before the ceiling clamped it
 * @param equilibrium level this gauge settles at if the recent rhythm holds, in {@code [0, 100]}
 */
@Schema(description = "One colony gauge, the day's movement on it, and the level it settles at.")
public record ColonyGaugeResponse(double value, double gain, double equilibrium) {
}
