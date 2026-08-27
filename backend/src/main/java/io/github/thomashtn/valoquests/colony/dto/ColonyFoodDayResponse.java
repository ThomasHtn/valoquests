package io.github.thomashtn.valoquests.colony.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * Exposes one day of the seven-day harvest the food stock is made of.
 *
 * <p>The stock is a rolling window, never a reserve, so the seven days behind it are the only honest
 * picture of it: they say <b>when</b> the squad played, which a single total cannot. It is also the one
 * reading of the food that is not already carried by the population figure — what the stock allows and
 * what the town holds are the same ratio seen twice.
 *
 * @param day     calendar day the harvest was brought in on
 * @param harvest food harvested that day, turnout multiplier included
 */
@Schema(description = "One day of the seven-day food harvest window.")
public record ColonyFoodDayResponse(
    LocalDate day,
    double harvest
) {
}
