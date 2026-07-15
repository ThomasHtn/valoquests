package io.github.thomashtn.valorant.tracker.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.*;

/**
 * Represents the API response payload for page response.
 */
@Schema(description = "API response model documented by the Valorant Tracker OpenAPI specification.")
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
}
