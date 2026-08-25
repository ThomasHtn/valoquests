package io.github.thomashtn.valoquests.colony.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes one gauge and what moved it today.
 *
 * @param value today's value, in {@code [0, 100]}
 * @param gain  what the day brought in, before the ceiling clamped it
 * @param loss  what the day cost, {@code 14 x (population / capacity)}
 */
@Schema(description = "One colony gauge and the day's movement on it.")
public record ColonyGaugeResponse(double value, double gain, double loss) {
}
