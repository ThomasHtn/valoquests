package io.github.thomashtn.valorant.tracker.synchronization.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import io.github.thomashtn.valorant.tracker.synchronization.model.*;
import java.time.*;

/**
 * Represents the API response payload for synchronization response.
 */
@Schema(description = "API response model documented by the Valorant Tracker OpenAPI specification.")
public record SynchronizationResponse(
    Long id,
    SynchronizationType type,
    SynchronizationTrigger trigger,
    SynchronizationStatus status,
    Instant startedAt,
    Instant finishedAt,
    Instant lastAttemptAt,
    Instant lastSuccessfulSynchronizationAt,
    int playersProcessed,
    int failureCount,
    int matchesImported,
    String errorMessage
) {
}
