package io.github.thomashtn.valoquests.run.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Carries the automatic-renewal setting an operator wants applied.
 */
@Schema(description = "Automatic-renewal setting to apply.")
public record CampaignAutoRenewUpdateRequest(

    boolean enabled
) {
}
