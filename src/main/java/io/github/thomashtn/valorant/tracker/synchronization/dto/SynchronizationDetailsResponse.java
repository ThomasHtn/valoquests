package io.github.thomashtn.valorant.tracker.synchronization.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import io.github.thomashtn.valorant.tracker.synchronization.model.*;
import java.time.*;
import java.util.*;

/**
 * Represents the API response payload for synchronization details response.
 */
@Schema(description = "API response model documented by the Valorant Tracker OpenAPI specification.")
public record SynchronizationDetailsResponse(
    Long id,
    SynchronizationType type,
    SynchronizationTrigger trigger,
    SynchronizationStatus status,
    Instant startedAt,
    Instant finishedAt,
    int playersProcessed,
    int failureCount,
    int matchesImported,
    String errorMessage,
    List<PlayerResultResponse> players
) {
    public record PlayerResultResponse(
        Long playerId,
        String displayName,
        SynchronizationStatus status,
        int pagesFetched,
        int matchesImported,
        String errorMessage
    ) {
    }
}
